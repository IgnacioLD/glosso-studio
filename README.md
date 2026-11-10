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

## Getting the Code

The repository lives on GitHub: https://github.com/IgnacioLD/glosso-studio (issues and pull requests go there).
The old GitLab project only hosts the downloadable model and sentence databases.

## Building

Requirements: Android Studio (or the Android SDK command-line tools) and JDK 17 or newer.

```bash
git clone https://github.com/IgnacioLD/glosso-studio.git
cd glosso-studio
./gradlew :androidApp:assembleFdroidDebug
```

The app has two product flavors: `fdroid` (no Google Play libraries) and `playstore` (adds the in-app review prompt).
Debug builds need no signing setup. Release builds read the signing key from `local.properties`
(`signing.storeFile`, `signing.storePass`, `signing.alias`, `signing.keyPass`) or the matching `SIGNING_*` environment variables.

The acoustic model (~300 MB), its vocabulary and the sentence databases are not in the repository. The app downloads them on first use
from the GitLab package registry (version pinned by `dataVersion` in `androidApp/src/main/java/me/shirobyte42/glosso/di/AppModule.kt`).
Run it on a device or emulator with API 26 or higher.

## Architecture Overview

The project follows a clean architecture pattern within the Kotlin Multiplatform structure:
- **`shared` module:** Contains the domain logic, repositories, and cross-platform use cases.
- **`androidApp` module:** Contains the Compose UI, Android-specific data implementations (Room, Audio), and the ONNX integration.

Known issues and planned work are listed in [docs/BACKLOG.md](docs/BACKLOG.md).

## Contributing

Bug reports and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Credits

- [wav2vec2-lv-60-espeak-cv-ft](https://huggingface.co/onnx-community/wav2vec2-lv-60-espeak-cv-ft-ONNX) acoustic model (Apache-2.0)
- [ai-pronunciation-trainer](https://github.com/Thiagohgl/ai-pronunciation-trainer) for the original pronunciation scoring approach (AGPL-3.0)
- [Qwen3-TTS](https://github.com/QwenLM/Qwen3-TTS) for reference audio generation
- [ONNX Runtime](https://github.com/microsoft/onnxruntime) and [Koin](https://insert-koin.io)

## License

This project is licensed under the **GNU Affero General Public License v3 (AGPLv3)**. See the [LICENSE](LICENSE) file for the full license text.

## F-Droid

Glosso Studio is designed to be compatible with F-Droid.
- **Metadata:** Located in `fastlane/metadata/android`.
- **Build Recipe:** `metadata/me.shirobyte42.glosso.yml` is provided as a reference for F-Droid inclusion.
