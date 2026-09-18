package me.shirobyte42.glosso.data.audio

import kotlin.math.sqrt

/**
 * Decides whether a recording is worth scoring.
 *
 * The acoustic model always produces a most-likely phone path, so silence and
 * room noise get transcribed into *something* and then scored - giving the
 * learner a meaningless number for an attempt they never made. Worse, those
 * fabricated phones were counted as missed sounds, polluting the weak-phoneme
 * statistics. Everything here exists to reject those attempts before scoring.
 *
 * Thresholds are deliberately conservative: rejecting a genuine attempt is far
 * more annoying than occasionally scoring a noisy one, so they only trigger on
 * clear silence, clips that are obviously too short, or frames the model is
 * uniformly unsure about.
 */
object SpeechGate {

    /** Below this RMS (on float samples in [-1, 1]) the clip is effectively silent. */
    const val MIN_RMS = 0.005f

    /** 250 ms at 16 kHz - anything shorter cannot be a sentence. */
    const val MIN_SAMPLES = 4_000

    /** Blank frames carry no phone; a real attempt emits well over this. */
    const val MIN_VOICED_FRAMES = 5

    /** Mean top-1 probability over voiced frames; flat distributions are noise. */
    const val MIN_MEAN_CONFIDENCE = 0.30f

    /**
     * A frame whose best phone is this unlikely is not emitted at all. Keeps the
     * decoder from inventing phones out of breath, clicks and room tone.
     * Deliberately low: genuinely quiet or coarticulated phones sit around
     * 0.3-0.5, so this only drops frames the model is clearly unsure about.
     */
    const val MIN_FRAME_CONFIDENCE = 0.25f

    /** A window counts as speech once it reaches this fraction of the loudest window. */
    private const val SILENCE_RELATIVE_THRESHOLD = 0.02f

    /** Silence trimming works on 20 ms windows. */
    private const val WINDOW_MS = 20

    /** Root-mean-square amplitude of float PCM samples. */
    fun rms(pcm: FloatArray): Float {
        if (pcm.isEmpty()) return 0f
        var sumSquares = 0.0
        for (sample in pcm) sumSquares += sample.toDouble() * sample
        return sqrt(sumSquares / pcm.size).toFloat()
    }

    /**
     * Returns only the span of [pcm] that actually contains speech, with a 20 ms
     * margin either side so onsets and offsets are not clipped.
     *
     * This matters twice over: leading/trailing silence otherwise dominates the
     * mean/variance normalisation the acoustic model depends on, and the model
     * tends to emit spurious phones while it listens to a quiet room. Trimming
     * fixes both. Returns an empty array when there is no speech at all.
     */
    fun trimSilence(pcm: FloatArray, sampleRate: Int = 16_000): FloatArray {
        if (pcm.isEmpty()) return pcm
        val window = (sampleRate * WINDOW_MS / 1000).coerceAtLeast(1)
        val frameCount = pcm.size / window
        if (frameCount == 0) return pcm

        val energy = FloatArray(frameCount) { f ->
            var sumSquares = 0.0
            val start = f * window
            for (i in start until start + window) {
                sumSquares += pcm[i].toDouble() * pcm[i]
            }
            sqrt(sumSquares / window).toFloat()
        }

        val peak = energy.max()
        if (peak <= 0f) return FloatArray(0)
        val threshold = maxOf(peak * SILENCE_RELATIVE_THRESHOLD, MIN_RMS)

        var first = 0
        while (first < frameCount && energy[first] < threshold) first++
        var last = frameCount - 1
        while (last >= first && energy[last] < threshold) last--
        if (last < first) return FloatArray(0)

        val startSample = (first * window - window).coerceAtLeast(0)
        val endSample = ((last + 1) * window + window).coerceAtMost(pcm.size)
        return pcm.copyOfRange(startSample, endSample)
    }

    fun isTooShort(sampleCount: Int): Boolean = sampleCount < MIN_SAMPLES

    fun isTooQuiet(rms: Float): Boolean = rms < MIN_RMS

    /**
     * True when the model emitted enough confident, non-blank frames for the
     * result to mean anything.
     */
    fun isSpeechLike(voicedFrames: Int, meanConfidence: Float): Boolean =
        voicedFrames >= MIN_VOICED_FRAMES && meanConfidence >= MIN_MEAN_CONFIDENCE
}
