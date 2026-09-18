package me.shirobyte42.glosso.data.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shirobyte42.glosso.domain.model.EspeakModelConfig
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Unified phoneme recognizer backed by the multilingual eSpeak-fine-tuned wav2vec2 model
 * (`onnx-community/wav2vec2-lv-60-espeak-cv-ft-ONNX`, int8 quantized).
 * Produces eSpeak IPA regardless of spoken language — no per-language swapping.
 */
class EspeakWav2Vec2Recognizer(
    private val context: Context
) : PhonemeRecognizer {

    private val TAG = "EspeakRecognizer"
    private val WORD_DELIMITER = "|"

    /**
     * Spacing modifiers that belong to the preceding phoneme: length, stress, aspiration,
     * rhoticity, palatalization, labialization, nasalization, voicelessness, release/hold marks.
     * Combining (non-spacing) diacritics are handled generically by `Character.getType`.
     */
    private val MODIFIER_PREFIXES = setOf(
        'ː', 'ˑ', 'ʰ', 'ʼ', 'ˈ', 'ˌ', 'ʲ', 'ʷ', 'ˠ', 'ˤ', '˞', 'ˀ'
    )

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var idToToken: Map<Int, String> = emptyMap()
    private var padId: Int = 0

    private val _modelState = MutableStateFlow(ModelState.UNINITIALIZED)
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    override var modelError: String? = null
        private set

    init { initialize() }

    override fun initialize() {
        if (ortSession != null) {
            _modelState.value = ModelState.READY
            return
        }
        _modelState.value = ModelState.LOADING
        try {
            val modelFile = File(context.filesDir, EspeakModelConfig.MODEL_FILE)
            val vocabFile = File(context.filesDir, EspeakModelConfig.VOCAB_FILE)

            if (!modelFile.exists() || !vocabFile.exists()) {
                Log.d(TAG, "Model assets not yet downloaded (model=${modelFile.exists()}, vocab=${vocabFile.exists()})")
                _modelState.value = ModelState.UNINITIALIZED
                return
            }

            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv?.createSession(modelFile.readBytes())
            val (tokens, pad) = loadVocab(vocabFile)
            idToToken = tokens
            padId = pad
            Log.d(TAG, "Espeak wav2vec2 initialized. Vocab size: ${idToToken.size}, pad id: $padId")
            _modelState.value = ModelState.READY
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize espeak recognizer", e)
            modelError = e.message ?: "Unknown error loading phoneme model."
            _modelState.value = ModelState.FAILED
        }
    }

    private fun loadVocab(file: File): Pair<Map<Int, String>, Int> {
        val map = mutableMapOf<Int, String>()
        val json = JSONObject(file.readText())
        var pad = 0
        val keys = json.keys()
        while (keys.hasNext()) {
            val token = keys.next()
            val id = json.getInt(token)
            map[id] = token
            if (token == "<pad>") pad = id
        }
        return map to pad
    }

    override fun recognize(base64Wav: String): String? {
        val session = ortSession ?: run {
            if (_modelState.value == ModelState.UNINITIALIZED) initialize()
            ortSession ?: return null
        }
        val env = ortEnv ?: return null

        return try {
            val wavBytes = Base64.decode(base64Wav, Base64.DEFAULT)
            if (wavBytes.size <= 44) return null

            val rawSampleCount = (wavBytes.size - 44) / 2
            if (rawSampleCount <= 0) return null
            val raw = FloatArray(rawSampleCount) { i ->
                val b0 = wavBytes[44 + i * 2].toInt() and 0xff
                val b1 = wavBytes[44 + i * 2 + 1].toInt() and 0xff
                ((b0 or (b1 shl 8)).toShort()).toFloat() / 32768f
            }

            // Trim the quiet room off both ends first. Otherwise leading/trailing
            // silence skews the mean/variance normalisation the model expects, and
            // the model emits spurious phones while it listens to nothing.
            val pcm = SpeechGate.trimSilence(raw)
            if (pcm.isEmpty()) {
                Log.d(TAG, "No speech above the noise floor - not scoring")
                return null
            }
            if (SpeechGate.isTooShort(pcm.size)) {
                Log.d(TAG, "Speech too short after trimming (${pcm.size} samples) - not scoring")
                return null
            }

            val mean = pcm.average().toFloat()
            var variance = 0.0
            for (x in pcm) variance += (x - mean).toDouble() * (x - mean)
            variance /= pcm.size
            val rms = sqrt(mean.toDouble() * mean + variance).toFloat()
            if (SpeechGate.isTooQuiet(rms)) {
                Log.d(TAG, "Recording is effectively silent (rms=$rms) - not scoring")
                return null
            }
            val std = sqrt(variance).toFloat().coerceAtLeast(1e-9f)
            val normalized = FloatArray(pcm.size) { i -> (pcm[i] - mean) / std }
            val numSamples = pcm.size

            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(normalized),
                longArrayOf(1L, numSamples.toLong())
            )
            val output = session.run(mapOf("input_values" to inputTensor))
            val outTensor = output[0] as OnnxTensor
            val logits = outTensor.floatBuffer.array()
            val shape = outTensor.info.shape
            val timeSteps = shape[1].toInt()
            val vocabSize = shape[2].toInt()

            // Greedy decode, keeping the top-1 probability per frame so we can
            // tell a confident transcription apart from noise.
            val rawIds = IntArray(timeSteps)
            val topProbs = FloatArray(timeSteps)
            for (t in 0 until timeSteps) {
                val base = t * vocabSize
                var maxIdx = 0
                var maxVal = Float.NEGATIVE_INFINITY
                for (v in 0 until vocabSize) {
                    val score = logits[base + v]
                    if (score > maxVal) { maxVal = score; maxIdx = v }
                }
                var expSum = 0.0
                for (v in 0 until vocabSize) expSum += exp((logits[base + v] - maxVal).toDouble())
                rawIds[t] = maxIdx
                topProbs[t] = (1.0 / expSum).toFloat()
            }

            var voicedFrames = 0
            var confidenceSum = 0.0
            for (t in 0 until timeSteps) {
                if (rawIds[t] == padId) continue
                voicedFrames++
                confidenceSum += topProbs[t]
            }
            if (voicedFrames == 0) {
                Log.d(TAG, "No non-blank frames - not scoring")
                return null
            }
            val meanConfidence = (confidenceSum / voicedFrames).toFloat()
            if (!SpeechGate.isSpeechLike(voicedFrames, meanConfidence)) {
                Log.d(TAG, "Not speech-like (voiced=$voicedFrames, confidence=$meanConfidence) - not scoring")
                return null
            }

            // CTC collapse, dropping blank frames and any frame the model was not
            // confident about. Without the confidence floor, breath and clicks
            // become phones and then get scored as mistakes.
            val filtered = mutableListOf<Int>()
            var prev = -1
            for (t in 0 until timeSteps) {
                val id = rawIds[t]
                if (id == prev) continue
                prev = id
                if (id == padId) continue
                if (topProbs[t] < SpeechGate.MIN_FRAME_CONFIDENCE) continue
                filtered.add(id)
            }

            val tokens = mutableListOf<String>()
            for (id in filtered) {
                val token = idToToken[id] ?: continue
                // Drop tokens containing digits — these are Mandarin tone markers
                // (e.g. a5, u2, ə1) the multilingual model occasionally hallucinates
                // on non-Chinese audio. None of our practice languages ever produce
                // them, so stripping them at runtime keeps transcription in a form
                // that matches the offline-extracted reference IPA.
                if (token.any { it.isDigit() }) continue
                if (shouldFuseToPrevious(token) && tokens.isNotEmpty()) {
                    tokens[tokens.lastIndex] = tokens.last() + token
                } else {
                    tokens.add(token)
                }
            }

            tokens.joinToString("") { if (it == WORD_DELIMITER) " " else it }
                .trim()
                .replace(Regex("\\s+"), " ")
        } catch (e: Exception) {
            Log.e(TAG, "Recognition failed", e)
            null
        }
    }

    private fun shouldFuseToPrevious(token: String): Boolean {
        if (token.isEmpty() || token == WORD_DELIMITER) return false
        val first = token[0]
        if (first in MODIFIER_PREFIXES) return true
        return Character.getType(first.code) == Character.NON_SPACING_MARK.toInt()
    }

    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            ortEnv = null
            idToToken = emptyMap()
            padId = 0
            _modelState.value = ModelState.UNINITIALIZED
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recognizer", e)
        }
    }
}
