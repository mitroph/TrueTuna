# TrueTuna — Android Guitar & Chromatic Tuner

A production-ready, zero-dependency native Android guitar and chromatic tuner app for Google Pixel 8 (Android 14/15/16, 120Hz OLED display).

## Features

- 🎸 **Guitar Guided Mode** — 15 tunings (Standard, Drop D, Open G, DADGAD, and more) with auto string detection or manual string lock
- 🎵 **Chromatic Mode** — Detects any note from B0 (30.87 Hz) to E6 (~1318 Hz)
- 🎯 **YIN Algorithm** — Pure Kotlin pitch detection with parabolic interpolation for sub-Hz accuracy
- 📐 **12-TET & 24-TET** — Quarter-tone microtonal detection with dedicated UI indicators
- 🟠 **OLED-optimized UI** — Deep black background, vibrant orange accent, animated Canvas gauge
- 🌍 **3 Languages** — Russian, Ukrainian, English (toggle in-app)
- 🔔 **Haptic Feedback** — Crisp vibration when locking in-tune
- 📱 **Screen Always On** — Keeps display alive during tuning sessions

## Project Structure

```
TrueTuna/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/truetuna/tuner/
│       │   ├── MainActivity.kt
│       │   ├── audio/
│       │   │   ├── AudioEngine.kt          ← AudioRecord + PCM float capture
│       │   │   └── PitchDetector.kt        ← YIN algorithm, EMA smoothing
│       │   ├── data/
│       │   │   └── TuningRepository.kt     ← 15 guitar tunings with frequencies
│       │   ├── model/
│       │   │   └── TuningModels.kt         ← Note math, 12/24-TET, data classes
│       │   ├── ui/
│       │   │   ├── Strings.kt              ← RU/UA/EN localization
│       │   │   ├── screen/
│       │   │   │   └── TunerScreen.kt      ← All Compose UI + Canvas gauge
│       │   │   └── theme/
│       │   │       ├── Theme.kt            ← OLED dark palette, orange brand
│       │   │       └── Typography.kt
│       │   └── viewmodel/
│       │       └── TunerViewModel.kt       ← MVVM state, audio pipeline
│       └── res/...
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
├── gradlew / gradlew.bat
└── .github/workflows/build-apk.yml
```

## Building the APK

### Prerequisites
- **Android Studio** (Hedgehog or newer) — [Download](https://developer.android.com/studio)
- **JDK 17** — bundled with Android Studio
- **Android SDK** with API 35 (Android 15)

### Option 1: Android Studio (Recommended)
1. Open Android Studio → **File → Open** → select the `TrueTuna` folder
2. Let Gradle sync complete
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Command Line
```bash
# Windows
gradlew.bat assembleDebug

# Linux/macOS (GitHub Actions)
./gradlew assembleDebug

# Output APK
app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: GitHub Actions (Automatic)
Push to `main` or `master` branch — the workflow at `.github/workflows/build-apk.yml` will:
1. Run unit tests (`testDebugUnitTest`)
2. Build both Debug and Release APKs (`assembleDebug assembleRelease`)
3. Upload them as a downloadable artifact named `TrueTuna-APKs`
4. Available from the **Actions** tab in your GitHub repository

## Tests

```bash
./gradlew testDebugUnitTest   # YIN accuracy (41 Hz–1.3 kHz), 12/24-TET note math, tuning table
```

## Installing on Pixel 8

```bash
# Via ADB (with USB Debugging enabled on your Pixel 8)
adb install app/build/outputs/apk/debug/app-debug.apk

# Or side-load: copy the APK to your phone and open it
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 + Canvas |
| Audio | `AudioRecord` (UNPROCESSED/MIC, PCM Float 44100 Hz) |
| Pitch Detection | YIN Algorithm (pure Kotlin, no native libs) |
| Architecture | Clean MVVM (ViewModel + StateFlow) |
| Build | Gradle Kotlin DSL (Gradle 8.9), AGP 8.7, minSdk 29, targetSdk 35 |

## Color Palette

| Purpose | Color |
|---------|-------|
| Background (OLED) | `#0D0D0F` |
| Surface | `#18181C` |
| Brand Orange | `#FF6D00` |
| Amber (close) | `#FFAB40` |
| Red-Orange (far) | `#FF3D00` |
| In-Tune Green | `#00E676` |

## Supported Tunings

**Standard & Drop:** Standard, Drop D, Double Drop D, Drop C, Eb/Half-Step Down, D Standard  
**Open:** Open D, Open G, Open E, Open A, Open C, Open F  
**Alternative:** DADGAD, Nick Drake CGCGCE, All Fourths
