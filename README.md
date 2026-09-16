# Glosso Studio

Glosso Studio is an offline-first pronunciation training application built with Kotlin Multiplatform and Jetpack Compose. It provides real-time phonetic assessment and a structured curriculum to help users improve their pronunciation in English (UK/US), French, Spanish, German and Latin.

## Features

### Phonetic Assessment
The application runs a **wav2vec2 acoustic model** (facebook/wav2vec2-lv-60-espeak-cv-ft, int8-quantized ONNX) directly on device via ONNX Runtime. It processes **16 kHz raw waveform** input (no MFCC feature extraction) and maps predictions to **eSpeak phoneme labels**, which are compared against the sentence's canonical eSpeak IPA using a language-specific phoneme similarity matrix (alignment, per-phoneme feedback and minimal-pair hints).

### Offline-First Architecture
The application is designed to be fully functional offline once the initial assets are retrieved.
- **ONNX Runtime:** Executes the acoustic model (`wav2vec2_espeak_cv_ft_int8.onnx`) directly on the device.
- **Dynamic Asset Download:** To keep the APK size minimal, the acoustic model, vocabulary and per-language curriculum databases are downloaded on-demand from the GitLab generic package registry when first needed.
- **Room Database:** Manages the repository of practice sentences and user progress.
- **Git LFS:** Used for managing large binary assets in the repository.

### Mastery and Progress Tracking
- **Curriculum Levels:** Six difficulty tiers (A1-C2) from Beginner to Mastery.
- **Mastery System:** Sentences are marked as mastered when users achieve a threshold score (85%+).
- **Spaced Repetition:** Mastered sentences enter a review queue with growing intervals.
- **Streak Tracking:** Encourages consistent practice through a daily streak system verified against activity logs.
- **Statistics:** Progress screen with mastery timeline, practice calendar, weakest phonemes and review backlog.

## Tech Stack

- **Framework:** Kotlin Multiplatform (KMP)
- **UI:** Jetpack Compose (Material 3)
- **Dependency Injection:** Koin
- **Database:** Room (Android)
- **Networking:** Ktor Client
- **Machine Learning:** ONNX Runtime for Android
- **Phonetic Model:** facebook/wav2vec2-lv-60-espeak-cv-ft (int8-quantized ONNX) with eSpeak phoneme labels
- **Text-to-Speech:** Android system `TextToSpeech` engine for reference playback
- **Audio Processing:** 16 kHz PCM capture, raw waveform model input
- **Serialization:** Kotlinx Serialization

## Prerequisites

- **Git LFS:** Required to pull the large binary assets in the repository.
- **Android Studio:** Hedgehog (2023.1.1) or later recommended.
- **JDK:** Version 17.

## Setup and Installation

1. **Install Git LFS**
   Ensure Git LFS is installed on your system before cloning:
   ```bash
   git lfs install
   ```

2. **Clone the Repository**
   ```bash
   git clone git@github.com:IgnacioLD/glosso-studio.git
   cd glosso-studio
   git lfs pull
   ```

3. **Open in Android Studio**
   Open the root directory as a project. Android Studio will automatically start the Gradle sync process and download necessary dependencies.

4. **Run the Application**
   Select the `androidApp` configuration and run it on a physical device or emulator (API 26 or higher).

## Architecture Overview

The project follows a clean architecture pattern within the Kotlin Multiplatform structure:
- **`shared` module:** Contains the domain logic, repositories, and cross-platform use cases.
- **`androidApp` module:** Contains the Compose UI, Android-specific data implementations (Room, Audio), and the ONNX integration.

## License

This project is licensed under the **GNU Affero General Public License v3 (AGPLv3)**. See the [LICENSE](LICENSE) file for the full license text.

## F-Droid

Glosso Studio is designed to be compatible with F-Droid.
- **Metadata:** Located in `fastlane/metadata/android`.
- **Build Recipe:** The `me.shirobyte42.glosso.yml` file is provided as a reference for F-Droid inclusion.
