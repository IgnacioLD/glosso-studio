package me.shirobyte42.glosso.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.shirobyte42.glosso.domain.model.languageConfigFor
import me.shirobyte42.glosso.domain.repository.PreferenceRepository

data class SettingsUiState(
    val playbackSpeed: Float = 1.0f,
    val isIpaVisible: Boolean = false,
    val isTranslationVisible: Boolean = true,
    val themeMode: Int = 0, // 0=system, 1=light, 2=dark
    val targetLanguage: String = "en_GB",
    val uiLanguage: String = "en",
    val showResetConfirmation: Boolean = false,
    val pendingLatinSwitch: Boolean = false
)

class SettingsViewModel(
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            playbackSpeed = prefs.getPlaybackSpeed(),
            isIpaVisible = prefs.isIpaVisible(),
            isTranslationVisible = prefs.isTranslationVisible(),
            themeMode = prefs.getThemeMode(),
            targetLanguage = prefs.getTargetLanguage(),
            uiLanguage = prefs.getUiLanguage()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        prefs.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setIpaVisible(visible: Boolean) {
        prefs.setIpaVisible(visible)
        _uiState.update { it.copy(isIpaVisible = visible) }
    }

    fun setTranslationVisible(visible: Boolean) {
        prefs.setTranslationVisible(visible)
        _uiState.update { it.copy(isTranslationVisible = visible) }
    }

    fun setThemeMode(mode: Int) {
        prefs.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setTargetLanguage(code: String) {
        // Intercept first-time Latin selection to surface the "experimental" dialog.
        if (code == "la" && !prefs.isLatinWarningAcknowledged()) {
            _uiState.update { it.copy(pendingLatinSwitch = true) }
            return
        }
        applyTargetLanguage(code)
    }

    fun confirmLatinWarning() {
        prefs.setLatinWarningAcknowledged(true)
        _uiState.update { it.copy(pendingLatinSwitch = false) }
        applyTargetLanguage("la")
    }

    fun dismissLatinWarning() {
        _uiState.update { it.copy(pendingLatinSwitch = false) }
    }

    private fun applyTargetLanguage(code: String) {
        prefs.setTargetLanguage(code)
        prefs.clearSavedBatch()
        _uiState.update { it.copy(targetLanguage = code) }
    }

    fun setUiLanguage(tag: String) {
        prefs.setUiLanguage(tag)
        _uiState.update { it.copy(uiLanguage = tag) }
        val locales = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                      else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun resetTutorial() {
        prefs.setTutorialShown(false)
    }

    fun requestResetProgress() {
        _uiState.update { it.copy(showResetConfirmation = true) }
    }

    fun dismissResetConfirmation() {
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun confirmResetProgress() {
        viewModelScope.launch {
            prefs.resetProgress()
            _uiState.update { it.copy(showResetConfirmation = false) }
        }
    }
}
