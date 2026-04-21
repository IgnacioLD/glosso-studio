package me.shirobyte42.glosso.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class GlossoTtsController(context: Context) {

    private val TAG = "GlossoTtsController"
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                          result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isReady) Log.e(TAG, "TTS language not supported")
                else Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun speak(word: String) {
        if (isReady) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "glosso_tts")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
