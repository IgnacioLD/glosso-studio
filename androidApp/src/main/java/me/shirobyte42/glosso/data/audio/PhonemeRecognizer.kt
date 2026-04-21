package me.shirobyte42.glosso.data.audio

import kotlinx.coroutines.flow.StateFlow

enum class ModelState { UNINITIALIZED, LOADING, READY, FAILED }

interface PhonemeRecognizer {
    fun recognize(base64Wav: String): String?
    fun initialize()
    val modelState: StateFlow<ModelState>
    val modelError: String?
}
