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
    private val repo: String = "IgnacioLD/glosso-studio"
) {
    private val TAG = "DatabaseDownloader"

    fun getDbName(levelIndex: Int): String {
        val practice = getPracticeLanguage().ifEmpty { "en_GB" }
        return "sentences_v11_${practice}_${levelIndex}.db"
    }

    private fun getBaseUrl(): String {
        // Binaries are published as GitHub Release assets (free, unlimited bandwidth).
        // URL shape: https://github.com/OWNER/REPO/releases/download/TAG/FILENAME
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val rawVersion = packageInfo.versionName.substringBefore("-")
        val version = if (rawVersion.startsWith("v")) rawVersion else "v$rawVersion"
        return "https://github.com/$repo/releases/download/$version"
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
                    val bytes = response.body<ByteArray>()
                    withContext(Dispatchers.IO) { vocabFile.writeBytes(bytes) }
                    if (vocabFile.length() < EspeakModelConfig.MIN_VOCAB_BYTES) {
                        vocabFile.delete()
                        throw Exception("Vocab download too small (${vocabFile.length()} bytes).")
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
                    onnxFile.delete()
                    throw Exception("Model download too small (${onnxFile.length()} bytes).")
                }
            }

            if (isModelSetupComplete()) {
                trySend(DownloadProgress.Success)
            } else {
                trySend(DownloadProgress.Error("Verification failed"))
            }
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Asset download failed", e)
            trySend(DownloadProgress.Error(e.message ?: "Setup failed"))
            close(e)
        }
        awaitClose { }
    }

    private suspend fun downloadStreamingInternal(url: String, destination: File, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
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

                        FileOutputStream(destination).use { fos ->
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
                            destination.delete()
                            throw Exception("File size mismatch after download!")
                        }
                    } else {
                        throw Exception("HTTP ${response.status.value}")
                    }
                }
            } catch (e: Exception) {
                if (destination.exists()) {
                    destination.delete()
                }
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
            trySend(DownloadProgress.Error(e.message ?: "Download failed"))
            close(e)
        }
        awaitClose { }
    }
}

sealed class DownloadProgress {
    data class Progress(val percent: Float) : DownloadProgress()
    object Success : DownloadProgress()
    data class Error(val message: String) : DownloadProgress()
}
