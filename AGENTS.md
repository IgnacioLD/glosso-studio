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
./gradlew test                                 # unit tests (currently none exist)
```

## Architecture quirks

- **DI:** Koin. Modules are `commonModule` (`shared`) + `appModule` (`androidApp`). ViewModels use `viewModel { ... }` with `parametersOf` for per-level DB wiring.
- **Room:** Two DB categories — a persistent `glosso_progress_db` (mastery, streaks, reviews) and per-level dynamic databases (`sentences_{level}.db`, downloaded on demand via `DatabaseDownloader`). Both use `fallbackToDestructiveMigration()`.
- **Model inference:** ONNX Runtime (`onnxruntime-android:1.24.3`). ML model file (`wav2vec2_espeak_cv_ft_int8.onnx`) lives in `data/` and is shipped as asset. Model assets are *not* bundled in APK — downloaded at runtime.
- **Product flavors:** `playstore` / `fdroid`. In-app review (`playstoreImplementation`) only compiled for playstore flavor.
- **Version:** `versionCode = 2208`, `versionName = "2.2.7"`. Version tag pipeline: GitLab CI builds tags, uploads APK + model files + sentence DBs to generic package registry.

## Important state / gotchas

- All profile/baseline-artprofile tasks are **disabled** in build config (non-deterministic).
- ProGuard keeps `ai.onnxruntime.**`, strips `org.slf4j` warnings.
- `MigratePrefLanguage` one-shot cleanup rewrites `target_language` pref from `en` → `en_GB` (v2.0 → v2.1 upgrade). Gated by `migrated_to_v11b` shared pref flag.
- JDK 17 required. Gradle JVM configured via `gradle.properties` pointing to Android Studio bundled JBR.
- MinSdk 26, targetSdk 35, compileSdk 34.
- Composable lint convention: all scrollable screens must use `Modifier.verticalScroll(rememberScrollState())` and `navigationBarsPadding()`.
- Theme: `GlossoTheme` in `me.shirobyte42.glosso.presentation.theme`. Supports light/dark via `themeMode` (0=system, 1=light, 2=dark).
- Data directory contains per-language sentence DBs (`sentences_v11_{lang}_{level}.db`). These are not in the APK — downloaded on demand.
- Existing instruction file: `GEMINI.md` (sibling to this file). Covers similar ground but includes a stale `UpdateUserProgressUseCase` reference — the actual class is `UpdateMasteryUseCase` in file `UpdateUserProgressUseCase.kt`.
- Git LFS required for large assets. Run `git lfs install && git lfs pull` after clone.
- GitLab CI config: `.gitlab-ci.yml` — builds on tags only, uploads APK + `data/*` files to GitLab generic package registry.
