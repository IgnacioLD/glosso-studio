# Glosso Studio — agent instructions

Kotlin Multiplatform + Jetpack Compose Android app for offline pronunciation training.

## Modules

- **`:shared`** — domain models, repository interfaces, use cases (`commonMain/kotlin/.../domain/`). No Android dependency.
- **`:androidApp`** — Compose UI, Room, ONNX Runtime, audio recording, Koin wiring. Entrypoint: `GlossoApp` → `MainActivity`.

## Commands

```sh
./gradlew assembleDebug                        # full build
./gradlew :androidApp:installDebug             # install on device/emulator
./gradlew lint                                 # lint check
./gradlew :androidApp:testFdroidDebugUnitTest # unit tests
```

## Architecture quirks

- **DI:** Koin. Modules are `commonModule` (`shared`) + `appModule` (`androidApp`). ViewModels use `viewModel { ... }` with `parametersOf` for per-level DB wiring.
- **Room:** Two DB categories — a persistent `glosso_progress_db` (mastery, streaks, reviews) and per-level dynamic databases (`sentences_{level}.db`, downloaded on demand via `DatabaseDownloader`). The progress DB never wipes user data on upgrade: destructive fallback is limited to legacy pre-v11 schemas (`fallbackToDestructiveMigrationFrom(1..10)`); any future version bump needs a real `Migration` (schemas exported to `androidApp/schemas/` via KSP for `MigrationTestHelper`). The level DBs keep full destructive fallback (re-downloadable cache semantics).
- **Model inference:** ONNX Runtime (`onnxruntime-android:1.24.3`). Model files (`wav2vec2_espeak_cv_ft_int8.onnx`, `espeak_vocab.json`) are *not* in git or the APK, downloaded at runtime from the GitLab package pinned by `dataVersion` in `AppModule.kt`.
- **Product flavors:** `playstore` / `fdroid`. In-app review (`playstoreImplementation`) only compiled for playstore flavor.
- **Version:** `versionCode = 2213`, `versionName = "2.3.3"`. Releases are built locally and attached to GitHub releases.

## Important state / gotchas

- All profile/baseline-artprofile tasks are **disabled** in build config (non-deterministic).
- ProGuard keeps `ai.onnxruntime.**`, strips `org.slf4j` warnings.
- `MigratePrefLanguage` one-shot cleanup rewrites `target_language` pref from `en` → `en_GB` (v2.0 → v2.1 upgrade). Gated by `migrated_to_v11b` shared pref flag.
- JDK 17+ required (also verified on JDK 21). The Gradle wrapper (`gradle/wrapper/gradle-wrapper.jar`, pinned to Gradle 8.14.3) is committed; Gradle resolves the JVM from `JAVA_HOME`/PATH. Do not re-add a machine-specific `org.gradle.java.home` to `gradle.properties`; F-Droid's metadata strips it anyway.
- MinSdk 26, targetSdk 36, compileSdk 36 (Play requires targetSdk 36 as of Nov 2026).
- Composable lint convention: all scrollable screens must use `Modifier.verticalScroll(rememberScrollState())` and `navigationBarsPadding()`.
- Theme: `GlossoTheme` in `me.shirobyte42.glosso.presentation.theme`. Supports light/dark via `themeMode` (0=system, 1=light, 2=dark).
- Data directory contains per-language sentence DBs (`sentences_v11_{lang}_{level}.db`). These are not in the APK — downloaded on demand.
- `UpdateMasteryUseCase` lives in the file `UpdateUserProgressUseCase.kt`.
- No Git LFS and no committed build outputs: large files are downloaded at runtime or kept as CI artifacts.
- The repo lives on GitHub (`IgnacioLD/glosso-studio`). The GitLab project `shirobyte421/glosso-studio` is kept only because installed apps download the model and sentence DBs from its package registry (`DatabaseDownloader`); do not delete it.
