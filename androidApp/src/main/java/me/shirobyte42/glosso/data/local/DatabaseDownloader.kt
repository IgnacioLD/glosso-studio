package me.shirobyte42.glosso.data.local

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import me.shirobyte42.glosso.domain.model.EspeakModelConfig

class DatabaseDownloader(
    private val context: Context,
    private val client: HttpClient,
    private val getPracticeLanguage: () -> String = { "en_GB" },
    private val getUiLanguage: () -> String = { "en" },
    private val gitlabProjectId: String = "80477548",
    private val gitlabPackageName: String = "glosso-studio",
    private val dataVersion: String? = null
) {
    private val TAG = "DatabaseDownloader"

    fun getDbName(levelIndex: Int): String {
        val practice = getPracticeLanguage().ifEmpty { "en_GB" }
        return "sentences_v11_${practice}_${levelIndex}.db"
    }

    private fun getBaseUrl(): String {
        // Binaries are published to GitLab Generic Package Registry.
        // URL shape: https://gitlab.com/api/v4/projects/ID/packages/generic/NAME/VERSION/FILENAME
        val version = dataVersion ?: run {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val rawVersion = packageInfo.versionName?.substringBefore("-") ?: "0.0.0"
            if (rawVersion.startsWith("v")) rawVersion else "v$rawVersion"
        }
        return "https://gitlab.com/api/v4/projects/$gitlabProjectId/packages/generic/$gitlabPackageName/$version"
    }

    private fun getDownloadUrl(fileName: String): String = "${getBaseUrl()}/$fileName"

    fun getDatabaseFile(levelIndex: Int): File = context.getDatabasePath(getDbName(levelIndex))
    fun getOnnxFile(): File = File(context.filesDir, EspeakModelConfig.MODEL_FILE)
    fun getVocabFile(): File = File(context.filesDir, EspeakModelConfig.VOCAB_FILE)

    fun isLevelDownloaded(levelIndex: Int): Boolean {
        val file = getDatabaseFile(levelIndex)
        return file.exists() && file.length() > 1024 * 1024
    }

    fun isModelSetupComplete(): Boolean {
        val onnxFile = getOnnxFile()
        val vocabFile = getVocabFile()
        return onnxFile.exists() && onnxFile.length() >= EspeakModelConfig.MIN_MODEL_BYTES &&
               vocabFile.exists() && vocabFile.length() >= EspeakModelConfig.MIN_VOCAB_BYTES
    }

    fun deleteLevel(levelIndex: Int) {
        val file = getDatabaseFile(levelIndex)
        if (file.exists()) file.delete()
    }

    fun downloadRequiredAssets(): Flow<DownloadProgress> = callbackFlow {
        try {
            // 1. Vocab JSON
            val vocabFile = getVocabFile()
            if (!vocabFile.exists() || vocabFile.length() < EspeakModelConfig.MIN_VOCAB_BYTES) {
                Log.d(TAG, "Downloading vocab (${EspeakModelConfig.VOCAB_FILE})...")
                try {
                    val response = client.get(getDownloadUrl(EspeakModelConfig.VOCAB_FILE))
                    if (!response.status.isSuccess()) {
                        throw DownloadException(
                            DownloadErrorKind.SERVER,
                            "HTTP ${response.status.value}"
                        )
                    }
                    val bytes = response.body<ByteArray>()
                    withContext(Dispatchers.IO) { vocabFile.writeBytes(bytes) }
                    if (vocabFile.length() < EspeakModelConfig.MIN_VOCAB_BYTES) {
                        val size = vocabFile.length()
                        vocabFile.delete()
                        throw DownloadException(
                            DownloadErrorKind.VERIFICATION,
                            "Vocab download too small ($size bytes)."
                        )
                    }
                } catch (e: Exception) {
                    if (vocabFile.exists()) vocabFile.delete()
                    throw e
                }
            }

            // 2. ONNX Model (streaming with progress)
            val onnxFile = getOnnxFile()
            if (!onnxFile.exists() || onnxFile.length() < EspeakModelConfig.MIN_MODEL_BYTES) {
                Log.d(TAG, "Downloading model (${EspeakModelConfig.MODEL_FILE})...")
                downloadStreamingInternal(getDownloadUrl(EspeakModelConfig.MODEL_FILE), onnxFile) { progress ->
                    trySend(DownloadProgress.Progress(progress))
                }
                if (onnxFile.length() < EspeakModelConfig.MIN_MODEL_BYTES) {
                    val size = onnxFile.length()
                    onnxFile.delete()
                    throw DownloadException(
                        DownloadErrorKind.VERIFICATION,
                        "Model download too small ($size bytes)."
                    )
                }
            }

            if (isModelSetupComplete()) {
                trySend(DownloadProgress.Success)
            } else {
                trySend(DownloadProgress.Error("Verification failed", DownloadErrorKind.VERIFICATION))
            }
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Asset download failed", e)
            trySend(DownloadProgress.Error(e.message ?: "Setup failed", classifyDownloadError(e)))
            close(e)
        }
        awaitClose { }
    }

    private suspend fun downloadStreamingInternal(url: String, destination: File, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val temporaryDestination = File(destination.parentFile, "${destination.name}.download")
            try {
                temporaryDestination.delete()
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength > 0) {
                            onProgress(bytesSentTotal.toFloat() / contentLength)
                        }
                    }
                }.execute { response ->
                    if (response.status.isSuccess()) {
                        val channel = response.bodyAsChannel()
                        val expectedSize = response.contentLength() ?: -1L
                        var totalBytesRead = 0L

                        FileOutputStream(temporaryDestination).use { fos ->
                            val bufferedOutputStream = fos.buffered()
                            val inputStream = channel.toInputStream()
                            val buffer = ByteArray(64 * 1024)
                            var bytes: Int

                            while (inputStream.read(buffer).also { bytes = it } != -1) {
                                bufferedOutputStream.write(buffer, 0, bytes)
                                totalBytesRead += bytes
                            }

                            bufferedOutputStream.flush()
                            fos.flush()
                            fos.getFD().sync()
                        }

                        if (expectedSize != -1L && totalBytesRead != expectedSize) {
                            temporaryDestination.delete()
                            throw DownloadException(
                                DownloadErrorKind.VERIFICATION,
                                "File size mismatch after download ($totalBytesRead of $expectedSize bytes)."
                            )
                        }
                        // Replace only after the complete file has been verified.
                        destination.delete()
                        if (!temporaryDestination.renameTo(destination)) {
                            throw DownloadException(
                                DownloadErrorKind.VERIFICATION,
                                "Could not finalize downloaded file."
                            )
                        }
                    } else {
                        throw DownloadException(
                            DownloadErrorKind.SERVER,
                            "HTTP ${response.status.value}"
                        )
                    }
                }
            } catch (e: Exception) {
                temporaryDestination.delete()
                throw e
            }
        }
    }

    fun downloadLevel(levelIndex: Int): Flow<DownloadProgress> = callbackFlow {
        val file = getDatabaseFile(levelIndex)
        file.parentFile?.mkdirs()
        val url = getDownloadUrl(getDbName(levelIndex))

        try {
            downloadStreamingInternal(url, file) { progress ->
                trySend(DownloadProgress.Progress(progress))
            }
            trySend(DownloadProgress.Success)
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Level download failed", e)
            trySend(DownloadProgress.Error(e.message ?: "Download failed", classifyDownloadError(e)))
            close(e)
        }
        awaitClose { }
    }
}

enum class DownloadErrorKind { NETWORK, SERVER, VERIFICATION, GENERIC }

class DownloadException(val kind: DownloadErrorKind, message: String) : Exception(message)

/**
 * Maps low-level failures to a coarse category so the UI can show a
 * localized, actionable message instead of a raw exception string.
 */
fun classifyDownloadError(e: Throwable): DownloadErrorKind {
    if (e is DownloadException) return e.kind
    return when (e) {
        is io.ktor.client.plugins.HttpRequestTimeoutException,
        is io.ktor.client.network.sockets.ConnectTimeoutException,
        is io.ktor.client.network.sockets.SocketTimeoutException,
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketException,
        is java.io.IOException -> DownloadErrorKind.NETWORK
        is io.ktor.client.plugins.ResponseException -> DownloadErrorKind.SERVER
        else -> DownloadErrorKind.GENERIC
    }
}

sealed class DownloadProgress {
    data class Progress(val percent: Float) : DownloadProgress()
    object Success : DownloadProgress()
    data class Error(val message: String, val kind: DownloadErrorKind) : DownloadProgress()
}
