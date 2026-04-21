package me.shirobyte42.glosso

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import me.shirobyte42.glosso.data.local.MigrationRunner
import me.shirobyte42.glosso.di.appModule
import me.shirobyte42.glosso.di.commonModule

class GlossoApp : Application() {
    override fun onCreate() {
        super.onCreate()

        MigrationRunner.runIfNeeded(this)
        applyPersistedLocale()

        startKoin {
            androidContext(this@GlossoApp)
            modules(commonModule, appModule)
        }
    }

    private fun applyPersistedLocale() {
        val prefs = getSharedPreferences("glosso_prefs", MODE_PRIVATE)
        val tag = prefs.getString("ui_language", null) ?: return
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == tag) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
