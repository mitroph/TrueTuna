# Role & Objective
Act as a Principal Android Engineer & Audio DSP (Digital Signal Processing) Specialist. Your task is to design and write a complete, production-ready, zero-dependency native Android guitar and chromatic tuner application named **"TrueTuna"** targeting modern devices (specifically optimized for **Google Pixel 8**, Android 14/15/16, 120Hz OLED display).

You must generate **complete, fully functional source code for every single file** without any `// TODO`, `// ... existing code`, or placeholder logic. The project must compile out-of-the-box into an installable `.apk` file.

---

# 1. Tech Stack & Architecture
- **Language:** Kotlin 2.0+ (Coroutines, StateFlow for reactive low-latency UI updates).
- **UI Framework:** Jetpack Compose (Material 3) + Custom `Canvas` drawing for 120fps smooth gauge/needle rendering.
- **Audio Capture:** Native `android.media.AudioRecord` using `AudioFormat.ENCODING_PCM_FLOAT`, `AudioSource.MIC` (or `UNPROCESSED` with fallback), Sample Rate `44100 Hz` or `48000 Hz`, Mono channel.
- **Architecture:** Clean MVVM (Single Activity, `TunerViewModel`, `AudioProcessor`, `PitchDetector`, `TuningRepository`).
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin 8.5+, `minSdk = 29`, `targetSdk = 35`.

---

# 2. Core Audio DSP & Pitch Detection Requirements
Do NOT use fake random data or external unavailable C++ libraries. Implement a rock-solid, pure Kotlin real-time pitch detection algorithm inside `PitchDetector.kt`:
1. **Algorithm:** Implement the **YIN algorithm** (or **McLeod Pitch Method - MPM**) with parabolic interpolation for sub-Hertz accuracy, capable of accurately tracking low frequencies down to `B0` (~30.87 Hz) and `E1` (~41.20 Hz) up to `E6` (~1318 Hz).
2. **Noise Gate & Smoothing:**
   - Calculate RMS (Root Mean Square) in dBFS. Ignore signals below a configurable noise threshold (default `-55 dBFS`).
   - Apply an Exponential Moving Average (EMA) filter or median buffer (3-5 frames) on the detected frequency so the UI needle moves smoothly without jittering.
3. **Microtonal / Quarter-Tone & Semitone Math (CRITICAL):**
   - Configurable reference pitch $A_4$ (default `440.0 Hz`, adjustable from `415 Hz` to `466 Hz`).
   - Total cents from $A_4$: 
     $$c = 1200 \cdot \log_2\left(\frac{f}{f_{A4}}\right)$$
   - Support two resolution modes selectable in UI:
     - **12-TET (Semitones / 100 cents step):** Standard notes (`C`, `C#`, `D`, `D#`, `E`, `F`, `F#`, `G`, `G#`, `A`, `A#`, `B`) with deviation in cents from `-50` to `+50`.
     - **24-TET (Quarter-tones / 50 cents step):** Microtonal detection showing standard notes AND quarter-tone steps (`C`, `C+¼`, `C#`, `C#+¼`, `D`, `D+¼`, etc., using clean notation like `C‡` / `C+` / `d` and explicit quarter-tone badges) with deviation from `-25` to `+25` cents, plus a secondary indicator on the main arc marking the `-50` and `+50` cent (quarter-tone) ticks at all times.

---

# 3. Functional Modes & Tuning Presets
Provide a seamless mode switcher on the main screen:

### Mode A: Free / Chromatic Mode (Strobe & Needle)
- Automatically detects any note played across the full spectrum.
- Displays:
  - Detected Note Name (large typography) + Octave number (e.g., `E2`).
  - Quarter-tone indicator (shows nearest quarter-tone step and highlights when exactly on a quarter-tone at $\pm 50$ cents).
  - Exact Frequency in Hz (to 0.1 Hz precision, e.g., `82.4 Hz`).
  - Deviation in Cents (e.g., `-3 ct`, `+12 ct`).

### Mode B: Guitar Guided Mode (Base & Open Tunings)
- Supports **Auto String Detection** (automatically locks onto the closest target string in the selected tuning) AND **Manual String Lock** (tapping a string peg button locks the tuner to that specific target frequency).
- Preload `TuningRepository.kt` with accurate frequencies for all strings across these categories:
  1. **Standard & Drop:**
     - Standard (`E2 A2 D3 G3 B3 E4`)
     - Drop D (`D2 A2 D3 G3 B3 E4`)
     - Double Drop D (`D2 A2 D3 G3 B3 D4`)
     - Drop C (`C2 G2 C3 F3 A3 D4`)
     - Eb / Half-Step Down (`Eb2 Ab2 Db3 Gb3 Bb3 Eb4`)
     - D Standard / Full-Step Down (`D2 G2 C3 F3 A3 D4`)
  2. **Open Tunings (Open N):**
     - Open D (`D2 A2 D3 F#3 A3 D4`)
     - Open G (`D2 G2 D3 G3 B3 D4`)
     - Open E (`E2 B2 E3 G#3 B3 E4`)
     - Open A (`E2 A2 E3 A3 C#4 E4`)
     - Open C (`C2 G2 C3 G3 C4 E4`)
     - Open F (`C2 F2 C3 F3 A3 C4`)
  3. **Alternative & Acoustic:**
     - DADGAD (`D2 A2 D3 G3 A3 D4`)
     - Nick Drake / CGCGCE (`C2 G2 C3 G3 C4 E4`)
     - All Fourths (`E2 A2 D3 G3 C4 F4`)

---

# 4. UI/UX Design & Orange Color Theme (Pixel 8 OLED Optimized)
- **Color Palette:**
  - Deep OLED Black / Charcoal background (`#0D0D0F` and surface `#18181C`) to save battery and maximize contrast on Pixel 8.
  - Primary Brand Accent: **Vibrant Electric Orange** (`#FF6D00` / `#FF8533`).
  - Secondary Warm Amber (`#FFAB40`) for slight detuning (`±5` to `±15` cents).
  - Sharp/Flat Warning Red-Orange (`#FF3D00`) when far out of tune (`> ±15` cents).
  - In-Tune Lock State: Glowing Neon Orange/Gold or Crisp Emerald accent (`#00E676` or `#FF9100` with outer glow ring) when within `±2.5` cents, accompanied by subtle haptic feedback (`HapticFeedbackType.LongPress` / Pixel crisp vibration when locking in tune).
- **Custom Compose Canvas Gauge:**
  - Draw a high-precision semicircular or horizontal linear scale from `-50` to `+50` cents.
  - Clearly mark `0` (center), `±25` cents, and `±50` cents (Quarter-Tone markers).
  - Smooth animated needle/cursor using `animateFloatAsState` with low stiffness/damping tuned for real-time audio.
  - Visual string selector at the bottom showing all 6 strings with their target note, current tuning status, and interactive tap-to-lock.
- **Keep Screen On:** Ensure `FLAG_KEEP_SCREEN_ON` is active while tuning so the phone doesn't sleep mid-tuning.
- **Permissions Handling:** Clean Compose UI state for requesting `Manifest.permission.RECORD_AUDIO` with rationale if denied.

---

# 5. Deliverables & Easy APK Generation
Output the complete project structure and every file needed to build the APK immediately:
1. `settings.gradle.kts`, root `build.gradle.kts`, and `app/build.gradle.kts` (configure debug and release signing so `./gradlew assembleDebug` produces a ready-to-install APK).
2. `app/src/main/AndroidManifest.xml` (with `RECORD_AUDIO` permission).
3. All Kotlin source files (`MainActivity.kt`, `PitchDetector.kt`, `AudioEngine.kt`, `TuningModels.kt`, `TunerViewModel.kt`, `TunerScreen.kt`, `Theme.kt`).
4. **GitHub Actions Workflow (`.github/workflows/build-apk.yml`):** Include a ready-to-use GitHub Actions workflow file that builds `app-debug.apk` on every push and uploads it as a downloadable artifact, plus provide the exact local CLI command (`./gradlew assembleDebug`) and path (`app/build/outputs/apk/debug/app-debug.apk`) so I can easily download and install it on my Pixel 8.
# 4.1. Localization & UI Language (IMPORTANT)
- **UI Text Language:** Make the user interface localized in **Russian** (and/or include an in-app quick language toggle between `RU`, `UA`, and `EN`).
- **Keep Musical Notation International:** Always keep note names (`C, C#, D, E, F, G, A, B`), frequency units (`Hz`, `ct`), and iconic tuning names (`Drop D`, `Open G`, `DADGAD`) in standard Latin notation so they remain clean and readable for guitarists, while translating UI labels, mode names, categories, tuning hints ("Ниже" / "В строе" / "Выше"), and permission dialogs.