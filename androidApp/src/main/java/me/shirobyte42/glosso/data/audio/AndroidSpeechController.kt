package me.shirobyte42.glosso.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Base64
import android.util.Log
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.LetterFeedbackModel
import me.shirobyte42.glosso.domain.model.MatchStatusModel
import me.shirobyte42.glosso.domain.model.PairHint
import me.shirobyte42.glosso.domain.model.PhonemeMatchModel
import me.shirobyte42.glosso.domain.model.PronunciationFeedback
import me.shirobyte42.glosso.domain.repository.SpeechController
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AndroidSpeechController(
    private val context: Context,
    private val recognizer: PhonemeRecognizer,
    private val getLanguage: () -> String,
    private val getUiLanguage: () -> String = { "en" }
) : SpeechController {

    private var wavRecorder: WavRecorder? = null
    private var player: MediaPlayer? = null
    private val audioFile = File(context.cacheDir, "speech_temp.wav")

    fun getContext(): Context = context

    override fun startRecording() {
        Log.d("SpeechController", "Starting WAV recording to ${audioFile.absolutePath}")
        try {
            stopPlayback()
            if (audioFile.exists()) {
                audioFile.delete()
            }

            // Unified eSpeak wav2vec2 model expects 16 kHz for every language.
            wavRecorder = WavRecorder(audioFile, 16000)
            wavRecorder?.start()
            Log.d("SpeechController", "WAV Recording started successfully")
        } catch (e: Exception) {
            Log.e("SpeechController", "Failed to start WAV recording", e)
            throw e
        }
    }

    override fun stopRecording(): String? {
        Log.d("SpeechController", "Stopping WAV recording")
        return try {
            wavRecorder?.stop()
            wavRecorder = null

            if (audioFile.exists()) {
                val size = audioFile.length()
                Log.d("SpeechController", "WAV file size: $size bytes")
                if (size > 44) {
                    val bytes = audioFile.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else {
                    Log.e("SpeechController", "WAV file is empty or only has header")
                    null
                }
            } else {
                Log.e("SpeechController", "WAV file does not exist after stop")
                null
            }
        } catch (e: Exception) {
            Log.e("SpeechController", "Failed to stop WAV recording", e)
            null
        }
    }

    override fun recognize(base64Wav: String): String? {
        return recognizer.recognize(base64Wav)
    }

    override fun calculateScore(targetText: String, expectedIpa: String, actualIpa: String): PronunciationFeedback {
        val language = getLanguage()
        val result = PhoneticComparator.calculateScoringResult(targetText, expectedIpa, actualIpa, language)
        val letterFeedback = PhoneticComparator.generateLetterFeedback(targetText, expectedIpa, result.alignment, language)

        // Generate pair hints for every mismatch (CLOSE + MISSED). For known
        // minimal pairs we use the curated linguistic description in the
        // learner's UI language; otherwise a localized generic fallback so the
        // learner always sees where they went wrong.
        val uiLang = getUiLanguage().ifEmpty { "en" }
        val seenPairs = mutableSetOf<Set<String>>()
        val pairHints = result.alignment
            .filter { it.expected != it.actual && it.expected.isNotBlank() && it.status != MatchStatus.PERFECT }
            .mapNotNull { match ->
                val pair = setOf(match.expected, match.actual)
                if (seenPairs.add(pair)) {
                    val description = PhoneticComparator.getMinimalPairDescription(match.expected, match.actual, uiLang)
                        ?: if (match.actual.isBlank() || match.actual == "-") {
                            context.getString(R.string.feedback_hint_missed, match.expected)
                        } else {
                            context.getString(R.string.feedback_hint_substitution, match.actual, match.expected)
                        }
                    PairHint(match.expected, match.actual, description)
                } else null
            }

        return PronunciationFeedback(
            score = result.score,
            transcription = actualIpa,
            normalizedExpected = result.normalizedExpected,
            normalizedActual = result.normalizedActual,
            alignment = result.alignment.map { match ->
                PhonemeMatchModel(
                    expected = match.expected,
                    actual = match.actual,
                    status = when (match.status) {
                        MatchStatus.PERFECT -> MatchStatusModel.PERFECT
                        MatchStatus.CLOSE -> MatchStatusModel.CLOSE
                        MatchStatus.MISSED -> MatchStatusModel.MISSED
                    }
                )
            },
            letterFeedback = letterFeedback.map { info ->
                LetterFeedbackModel(
                    char = info.char,
                    status = when (info.status) {
                        MatchStatus.PERFECT -> MatchStatusModel.PERFECT
                        MatchStatus.CLOSE -> MatchStatusModel.CLOSE
                        MatchStatus.MISSED -> MatchStatusModel.MISSED
                    }
                )
            },
            pairHints = pairHints
        )
    }

    override fun getNormalizedPhonemes(ipa: String): String {
        return PhoneticComparator.normalize(ipa)
    }

    override fun playAudio(base64: String, speed: Float, onComplete: (() -> Unit)?) {
        try {
            stopPlayback()
            val pureBase64 = if (base64.contains(",")) base64.substringAfter(",") else base64
            val audioData = Base64.decode(pureBase64, Base64.DEFAULT)

            val tempFile = File.createTempFile("native_audio", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }

            player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().setSpeed(speed)
                }
                start()
                setOnCompletionListener {
                    tempFile.delete()
                    onComplete?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechController", "Failed to play audio", e)
        }
    }

    /**
     * Plays user recording with RMS volume normalization so it matches
     * the reference voice loudness. The 44-byte WAV header is preserved.
     */
    override fun playNormalized(base64: String, onComplete: (() -> Unit)?) {
        try {
            stopPlayback()
            val pureBase64 = if (base64.contains(",")) base64.substringAfter(",") else base64
            val rawBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val normalizedBytes = if (rawBytes.size > 44) normalizeWavPcm(rawBytes) else rawBytes

            val tempFile = File.createTempFile("norm_audio", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(normalizedBytes) }

            player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempFile.delete()
                    onComplete?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechController", "Failed to play normalized audio", e)
            // Fallback to non-normalized playback
            playAudio(base64, 1.0f, onComplete)
        }
    }

    /**
     * RMS-normalizes the PCM data in a WAV byte array.
     * The 44-byte WAV header is preserved as-is.
     * Target RMS ~3000 out of max Short 32767. Gain is clamped to [0.5, 4.0].
     */
    private fun normalizeWavPcm(wavBytes: ByteArray): ByteArray {
        val headerSize = 44
        val pcmSize = wavBytes.size - headerSize
        if (pcmSize < 2) return wavBytes

        // Decode little-endian 16-bit PCM samples
        val sampleCount = pcmSize / 2
        val samples = ShortArray(sampleCount) { i ->
            val byteIndex = headerSize + i * 2
            ((wavBytes[byteIndex + 1].toInt() shl 8) or (wavBytes[byteIndex].toInt() and 0xFF)).toShort()
        }

        // Compute RMS
        val sumSquares = samples.fold(0.0) { acc, s -> acc + s.toDouble() * s.toDouble() }
        val rms = sqrt(sumSquares / sampleCount)
        if (rms < 1.0) return wavBytes // silence, skip normalization

        val targetRms = 3000.0
        val gain = (targetRms / rms).coerceIn(0.5, 4.0)

        // Apply gain and rebuild bytes
        val result = wavBytes.copyOf()
        for (i in 0 until sampleCount) {
            val normalized = (samples[i] * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            val byteIndex = headerSize + i * 2
            result[byteIndex] = (normalized.toInt() and 0xFF).toByte()
            result[byteIndex + 1] = ((normalized.toInt() shr 8) and 0xFF).toByte()
        }
        return result
    }

    override fun playWordSlice(fullBase64Wav: String, startSec: Float, endSec: Float, onComplete: (() -> Unit)?) {
        try {
            stopPlayback()
            val pureBase64 = if (fullBase64Wav.contains(",")) fullBase64Wav.substringAfter(",") else fullBase64Wav
            val wavBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            if (wavBytes.size <= 44) { onComplete?.invoke(); return }

            val sampleRate = java.nio.ByteBuffer.wrap(wavBytes, 24, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).int
            val bitsPerSample = java.nio.ByteBuffer.wrap(wavBytes, 34, 2)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()
            val channels = java.nio.ByteBuffer.wrap(wavBytes, 22, 2)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()
            val bytesPerSample = (bitsPerSample / 8).coerceAtLeast(1)
            val bytesPerSec = sampleRate * channels * bytesPerSample

            var startByte = (44 + startSec * bytesPerSec).toInt()
            if (bytesPerSample > 1) startByte = startByte and -bytesPerSample
            startByte = startByte.coerceAtLeast(44)
            val endByte = minOf((44 + endSec * bytesPerSec).toInt(), wavBytes.size)
            if (endByte <= startByte) { onComplete?.invoke(); return }

            val sliceSize = endByte - startByte
            val out = ByteArray(44 + sliceSize)
            System.arraycopy(wavBytes, 0, out, 0, 44)
            java.nio.ByteBuffer.wrap(out, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(36 + sliceSize)
            java.nio.ByteBuffer.wrap(out, 40, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(sliceSize)
            System.arraycopy(wavBytes, startByte, out, 44, sliceSize)

            val tempFile = File.createTempFile("word_slice", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(out) }

            player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempFile.delete()
                    onComplete?.invoke()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SpeechController", "playWordSlice failed", e)
            onComplete?.invoke()
        }
    }

    override fun playUrl(url: String, speed: Float) {
        try {
            stopPlayback()
            player = MediaPlayer().apply {
                setDataSource(url)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().setSpeed(speed)
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("SpeechController", "Failed to play URL", e)
        }
    }

    override fun stopPlayback() {
        try {
            player?.stop()
            player?.release()
            player = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
