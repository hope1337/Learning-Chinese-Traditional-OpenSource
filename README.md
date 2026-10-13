# Traditional Chinese Learning — Offline App for Learners in Taiwan

Native Android app (Kotlin + Jetpack Compose) that helps learners read
Traditional Chinese in daily life in Taiwan: take a photo, extract Chinese
text (OCR), check meanings and stroke order, save flashcards. **Fully usable
offline after install.**

This repository (`Chinese2`) is the clean, English-first, open-source
successor of a Vietnamese-language prototype. It is intentionally scoped for
open-source contribution around one deep-learning component:
**text recognition (OCR)**.

## Quick links

- `CONTRIBUTING.md` — how to contribute (app and models).
- `models/CONTRIBUTING-MODELS.md` — model-only contribution path (no Android needed).
- `docs/ARCHITECTURE.md` — system overview.
- `docs/STORAGE.md` — bundled MOE reference data vs. uninstall-surviving user data.
- `docs/MODELS.md` — OCR model contracts and evaluation.
- `docs/MOE_PROVENANCE.md` — Taiwan MOE data sources, licenses, attribution.
- `docs/COURSE_ENGAGEMENT.md` — how this repo maps to a deep-learning course
  open-source engagement component (public issues/discussions/PRs + reflection).

## Features (minimal viable scope)

- **Photo OCR**: camera / gallery, region-of-interest (ROI) selection before
  recognition, on-device engines (ML Kit Chinese + Paddle PP-OCRv5 via ONNX).
- **Taiwan MOE reference data, bundled**: dictionary entries, stroke-order
  geometry, Pinyin/Zhuyin readings. No network, no account, no backend.
- **Flashcards**: save words/sentences from confirmed OCR text, simple SRS
  review (Again/Hard/Good/Easy).
- **Pronunciation**: system TTS with offline Chinese voice when available.
- **Uninstall-surviving user data**: reference data ships in the APK and is
  restored by reinstall; user data (cards, history, owned images) lives in a
  user-selected folder (Storage Access Framework) that remains on disk after
  uninstall and can be re-linked on reinstall. Explicit `Delete all data`
  wipes it. See `docs/STORAGE.md`.

## Build

Requirements: Android Studio, Android SDK, JDK 17.

```powershell
.\gradlew.bat :app:assembleDebug
```

Unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Model checks (PC only, no Android needed):

```powershell
python tools/check_manifest.py
python tools/eval_ocr.py --help
```

## Data and licenses

- App code: MIT (see `LICENSE`).
- Taiwan Ministry of Education (MOE) stroke-order and dictionary sources:
  **CC BY-NC-ND 3.0 Taiwan**, non-commercial, no derivatives, attribution
  required. See `licenses/MOE_NOTE.md` and `docs/MOE_PROVENANCE.md`.
- MOE bytes are **not committed** to git (see `.gitignore`). They are fetched
  on the maintainer PC with `tools/fetch_moe.py` and bundled into the APK at
  build time. Missing assets degrade gracefully (lookup fallback, stroke view
  shows "Not available").

## Privacy and offline behavior

No account, no backend, no analytics. OCR, lookup, flashcards, and stroke
rendering run on-device. The only network exception is an explicit opt-in
online translation mode, if enabled in the future.
