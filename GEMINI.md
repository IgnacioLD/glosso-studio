# Glosso Studio Project Instructions

Glosso Studio is an offline-first pronunciation training application built with Kotlin Multiplatform (KMP) and Jetpack Compose. It uses the Allosaurus phonetic model for real-time assessment and Qwen3-TTS for speech synthesis.

## Project Overview

-   **Architecture:** Clean Architecture with a `shared` module for domain logic and repositories, and an `androidApp` module for the Compose UI and Android-specific implementations.
-   **Core Modules:**
    -   `:shared`: Common business logic, models, and repository interfaces.
    -   `:androidApp`: Android-specific UI (Jetpack Compose), Room database, and ML inference (ONNX Runtime).
-   **Key Technologies:**
    -   **UI:** Jetpack Compose (Android)
    -   **DI:** Koin
    -   **Networking:** Ktor
    -   **Database:** Room (on Android)
    -   **ML:** ONNX Runtime with Allosaurus `eng2102` model.
    -   **Audio:** Custom MFCC implementation and WAV recording.

## Development Workflows

### Setup
-   **Git LFS:** Required for large ONNX models and databases. Run `git lfs install` and `git lfs pull`.
-   **JDK:** Version 17 is required.

### Building and Running
-   Build: `./gradlew assembleDebug`
-   Run: `./gradlew :androidApp:installDebug` (requires a connected device or emulator)
-   Lint: `./gradlew lint`

### Testing
-   Run unit tests: `./gradlew test` (Note: No tests were found in the initial exploration, adding them is a priority).

## UI Conventions

-   **Scrolling:** Always ensure screens with potentially long content use `Modifier.verticalScroll(rememberScrollState())`.
-   **Padding:** Use `navigationBarsPadding()` on main containers to avoid content being obscured by the system navigation bar.
-   **Theme:** Follow the `GlossoTheme` defined in `me.shirobyte42.glosso.presentation.theme`.

## Data Management

-   **Curriculum:** Sentences are stored in a Room database. Levels are downloaded on-demand from GitLab.
-   **Preferences:** Managed via `PreferenceRepository`, implemented by `AndroidPreferenceRepository`.

## Local Development Notes (Private)
*Refer to the private MEMORY.md for machine-specific setups or credentials.*
