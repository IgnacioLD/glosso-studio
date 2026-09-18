package me.shirobyte42.glosso.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import me.shirobyte42.glosso.R
import me.shirobyte42.glosso.domain.model.MasteryLevel
import me.shirobyte42.glosso.domain.repository.PreferenceRepository

/**
 * Plays the short chime for a take's mastery level.
 *
 * Every level has its own sound: a bright melodic fanfare for a flawless take,
 * a warm triad for a mastered one, a gentle pair of notes for "almost", and two
 * soft descending minor cues for the lower levels. The "sad" ones are kept
 * quiet and round on purpose - they signal "keep trying", they never punish.
 */
class FeedbackSoundPlayer(
    private val context: Context,
    private val prefs: PreferenceRepository
) {
    private val TAG = "FeedbackSoundPlayer"
    private var player: MediaPlayer? = null

    private companion object {
        /**
         * 0..1 linear playback volume. The chimes are mastered loud (near full
         * scale); playing them at full volume is jarring right after a take, so
         * keep them well under the voice playback level.
         */
        const val PLAYBACK_VOLUME = 0.35f
    }

    fun play(level: MasteryLevel) {
        if (!prefs.isFeedbackSoundsEnabled()) return
        val resId = when (level) {
            MasteryLevel.PERFECT -> R.raw.feedback_perfect
            MasteryLevel.MASTERED -> R.raw.feedback_mastered
            MasteryLevel.ALMOST -> R.raw.feedback_almost
            MasteryLevel.ROUGH -> R.raw.feedback_rough
            MasteryLevel.NOT_YET -> R.raw.feedback_not_yet
        }
        try {
            release()
            player = MediaPlayer.create(context, resId)?.apply {
                setVolume(PLAYBACK_VOLUME, PLAYBACK_VOLUME)
                setOnCompletionListener { mp ->
                    mp.release()
                    if (player === mp) player = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play feedback sound", e)
            release()
        }
    }

    fun release() {
        try {
            player?.release()
        } catch (_: Exception) {
            // Already released or in an invalid state; nothing to do.
        }
        player = null
    }
}
