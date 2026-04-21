package me.shirobyte42.glosso.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import me.shirobyte42.glosso.data.audio.EspeakWav2Vec2Recognizer
import me.shirobyte42.glosso.data.audio.PhonemeRecognizer
import me.shirobyte42.glosso.data.audio.AndroidSpeechController
import me.shirobyte42.glosso.data.audio.GlossoTtsController
import me.shirobyte42.glosso.data.prefs.AndroidPreferenceRepository
import me.shirobyte42.glosso.domain.repository.PreferenceRepository
import me.shirobyte42.glosso.domain.repository.SpeechController
import me.shirobyte42.glosso.presentation.home.HomeViewModel
import me.shirobyte42.glosso.presentation.studio.StudioViewModel
import me.shirobyte42.glosso.presentation.settings.SettingsViewModel

import me.shirobyte42.glosso.data.local.LocalSentenceDataSource
import me.shirobyte42.glosso.data.local.GlossoDatabase
import me.shirobyte42.glosso.data.local.DatabaseDownloader
import me.shirobyte42.glosso.data.local.SentenceDao
import androidx.room.Room
import me.shirobyte42.glosso.data.repository.GlossoRepositoryImpl
import me.shirobyte42.glosso.domain.repository.GlossoRepository

import io.ktor.client.*
import io.ktor.client.plugins.*

val appModule = module {
    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = null
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 300000
            }
        }
    }

    single {
        val prefs: PreferenceRepository = get()
        DatabaseDownloader(
            context = get(),
            client = get(),
            getPracticeLanguage = { prefs.getTargetLanguage() },
            getUiLanguage = { prefs.getUiLanguage() }
        )
    }

    // Persistent database for user progress (streaks, activity, mastered IDs)
    single(qualifier = org.koin.core.qualifier.named("progress_db")) {
        Room.databaseBuilder(
            get(),
            GlossoDatabase::class.java,
            GlossoDatabase.PROGRESS_DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    // Factory for dynamic sentence databases (language-aware DB name)
    factory(qualifier = org.koin.core.qualifier.named("level_db")) { (levelIndex: Int) ->
        val downloader: DatabaseDownloader = get()
        Room.databaseBuilder(
            get(),
            GlossoDatabase::class.java,
            downloader.getDbName(levelIndex)
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    single { get<GlossoDatabase>(qualifier = org.koin.core.qualifier.named("progress_db")).masteredSentenceDao }
    single { get<GlossoDatabase>(qualifier = org.koin.core.qualifier.named("progress_db")).activityDayDao }
    single { get<GlossoDatabase>(qualifier = org.koin.core.qualifier.named("progress_db")).reviewDao }

    single<PhonemeRecognizer> { EspeakWav2Vec2Recognizer(get()) }
    single { GlossoTtsController(get()) }
    single<PreferenceRepository> { AndroidPreferenceRepository(get(), get(), get(), get()) }
    single<SpeechController> {
        AndroidSpeechController(
            get(), get(),
            get<PreferenceRepository>()::getTargetLanguage,
            get<PreferenceRepository>()::getUiLanguage,
        )
    }

    single {
        LocalSentenceDataSource(get()) { levelIndex ->
            val db: GlossoDatabase = get(qualifier = org.koin.core.qualifier.named("level_db")) { parametersOf(levelIndex) }
            db.sentenceDao
        }
    }

    single<GlossoRepository> { GlossoRepositoryImpl(get<LocalSentenceDataSource>()) }

    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { (levelIndex: Int) ->
        val levelDb: GlossoDatabase = get(qualifier = org.koin.core.qualifier.named("level_db")) { parametersOf(levelIndex) }

        StudioViewModel(
            repository = get(),
            sentenceDao = levelDb.sentenceDao,
            speechController = get(),
            prefs = get(),
            updateMastery = get(),
            recognizer = get(),
            ttsController = get(),
            appContext = androidContext()
        )
    }
}
