# 🎵 TuneUp

> A smart Android app that listens to your voice, compares it against a reference track, and shows you exactly where you went off-pitch — in real time.

TuneUp is a native Android application built with **Kotlin** and **Jetpack Compose**. It records your singing through the device microphone, detects pitch live using the **YIN algorithm**, aligns your performance with a reference audio using **Dynamic Time Warping (DTW)**, and gives you a score from 0 to 100. All processing happens **on-device** — no audio ever leaves your phone.

---

## ✨ Features

- 🎤 **Live pitch tracking** — real-time YIN-based pitch detection at 50 Hz update rate.
- 📊 **Dual pitch chart** — reference and your voice drawn on the same timeline.
- 🎯 **Intelligent scoring** — DTW aligns your timing with the reference, then scores based on cents deviation (< 50¢ counts as correct).
- 📁 **Any audio format** — MP3, WAV, M4A, OGG, FLAC via `MediaExtractor` + `MediaCodec`.
- 💾 **Session history** — every attempt (score, errors, recording) saved locally with Room.
- 🔒 **Private by design** — all audio processing is offline; nothing is uploaded.
- 🎧 **Background recording** — Foreground Service keeps the mic alive when the app is minimized.
- 🌙 **Material 3** — follows system dark/light theme.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Audio I/O | `AudioRecord`, `MediaCodec`, `MediaExtractor`, `MediaPlayer` |
| Pitch detection | Custom YIN implementation in Kotlin |
| Comparison | Dynamic Time Warping (DTW) on cent-scaled frequencies |
| Persistence | Room + Gson `TypeConverter` |
| Concurrency | Kotlin Coroutines + Flow |
| Build | Gradle 8.7, AGP 8.5, JDK 17 |
| CI | GitHub Actions (auto-builds APK on every push) |

---

## 🏗 Architecture

```
UI (Compose)
    └─> MainViewModel (StateFlow<PracticeUiState>)
            ├─> AudioRecorder ──> PitchDetector (YIN)
            ├─> ReferencePitchExtractor
            ├─> DtwComparator
            └─> PracticeSessionDao (Room)
```

Pitch is sampled in a background coroutine every ~20 ms. When recording stops, the user's pitch sequence and the reference's pitch sequence are aligned by DTW, and the percentage of frames within 50 cents becomes the final score.

---

## 🚀 Build & Install

### Option A — GitHub Actions (no local setup needed)

This repo includes a workflow at `.github/workflows/android-ci.yml` that builds the APK automatically on every push.

1. Fork or clone this repository.
2. Push your changes to `main` or `master`.
3. Go to the **Actions** tab → open the latest **Build TuneUp APK** run.
4. Scroll down to **Artifacts** → download **`tuneup-debug-apk`**.
5. Extract the ZIP and install `app-debug.apk` on your phone.

You can also trigger a build manually via **Actions → Build TuneUp APK → Run workflow**.

### Option B — Android Studio (local)

1. Open the project in Android Studio (Hedgehog or newer).
2. Let Gradle sync.
3. Connect a device or start an emulator (API 24+).
4. Press **Run ▶**.

To build an APK from the terminal:

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Running Tests

```bash
./gradlew testDebugUnitTest
```

The test suite covers:

- **`PitchDetectorTest`** — YIN accuracy on synthetic sine waves (440 Hz, 100 Hz, silence).
- **`DtwComparatorTest`** — identical sequences score ~100, octave-off scores <20, empty input returns 0.

---

## 📱 How to Use

1. Launch **TuneUp**.
2. Tap **Choose Reference Audio** and pick any song or vocal clip.
3. Grant microphone permission when prompted.
4. Tap **Start Recording** — the reference plays while your pitch is charted live.
5. Tap **Stop & Score** — your recording is saved and a score appears.
6. Open **History** (top-right icon) to review past attempts and track your progress.

> 💡 **Tip:** Wear headphones. Playing the reference through the speaker leaks into the microphone and reduces scoring accuracy.

---

## 🔐 Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Capture your voice for pitch analysis |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Keep recording when app is in background (Android 14 requirement) |
| `POST_NOTIFICATIONS` | Show the recording notification (Android 13+) |

No internet permission is requested. The app works entirely offline.

---

## 🗺 Roadmap

- [ ] Add note names (C4, D4…) to the pitch chart Y-axis
- [ ] Highlight specific mistakes on the chart after scoring
- [ ] Support for backing tracks with lyrics (karaoke mode)
- [ ] Export sessions as PDF or share score cards
- [ ] Replace YIN with a neural pitch model (CREPE via LiteRT) for noisy environments
- [ ] Migrate DTW to a banded implementation for very long recordings

---

## ⚠️ Known Limitations

- **Acoustic feedback** — the reference track playing through the speaker can bleed into the mic. Use headphones for best results.
- **Long recordings** — DTW memory grows quadratically. Sessions over ~3 minutes may cause memory pressure on low-end devices.
- **Noise sensitivity** — YIN requires a reasonably clean signal. For noisy environments, the neural pitch model is on the roadmap.
- **Package name** — currently `com.tuneup.app`; must be changed before publishing to Google Play.

---

## 📄 License

This project is released under the **MIT License**. See [`LICENSE`](LICENSE) for details.

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change.

---

## 🇮🇷 دربارهٔ TuneUp (فارسی)

**TuneUp** یک اپلیکیشن اندروید است که صدای آواز شما را با یک قطعه مرجع مقایسه می‌کند و به‌صورت زنده نشان می‌دهد کجا از نُت خارج شده‌اید. در پایان، امتیازی از ۰ تا ۱۰۰ به شما می‌دهد و اشتباهات را ذخیره می‌کند تا روند پیشرفتتان را ببینید.

تمام پردازش صدا **روی خود گوشی** انجام می‌شود و هیچ داده‌ای به سرور ارسال نمی‌شود.

### ساخت و نصب سریع

1. کد را روی GitHub خودتان push کنید.
2. به تب **Actions** بروید و منتظر بمانید.
3. از بخش **Artifacts**، فایل **`tuneup-debug-apk`** را دانلود کنید.
4. روی گوشی نصب کنید و لذت ببرید!

> 🎧 برای دقت بیشتر در امتیازدهی، حتماً از هدفون استفاده کنید.

---

**Built with ❤️ using Kotlin, Jetpack Compose, and the YIN pitch algorithm.**
