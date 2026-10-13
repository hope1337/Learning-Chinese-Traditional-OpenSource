# Project rules

These instructions apply to the entire repository.

## Platform and audience

- Native Android app only, Kotlin + Jetpack Compose.
- Android-first design and implementation.
- Audience: learners of Traditional Chinese in Taiwan (international students and others).
- All repository text (code, docs, issues, UI) is English. Traditional Chinese
  content itself is data and must be preserved exactly: never simplify,
  normalize, or convert it unless explicitly requested.
- No Flutter or React Native.

## Architecture and scope

- Offline-first. No backend, no auth, no analytics unless explicitly requested.
- Keep the architecture simple and incremental. Do not refactor unrelated code.
- The deep-learning contribution surface is isolated under `models/` with a
  stable contract (`docs/MODELS.md`). App code consumes models through the
  `OcrEngine` interface only.

## Data

- Taiwan MOE is the authority for dictionary and stroke-order reference data.
  Record source, license, URL, and SHA-256 per dataset (`docs/MOE_PROVENANCE.md`).
- Never silently mix PRC and Taiwan stroke or lexical standards.
- MOE bytes are never committed. Fetch on the PC (`tools/fetch_moe.py`),
  bundle into `app/src/main/assets/` at build time.
- Reference data (bundled, reinstallable) and user data (irreplaceable,
  uninstall-surviving via a user-selected SAF folder) are separate. See
  `docs/STORAGE.md`.

## OCR and language pipeline

- Camera and gallery share ROI selection before OCR. Default is the full
  image. Preview resolution is never used as crop resolution; map crops to
  original-resolution pixels with EXIF/rotation handling.
- Keep raw OCR, spatial blocks, engine provenance, and user-confirmed text separate.
- OCR and translation are separate stages. Only user-confirmed Chinese text
  may start translation or dictionary analysis.
- Never present concatenated character glosses as a sentence translation.

## Validation

- Run `.\gradlew.bat :app:assembleDebug` from the project root in PowerShell
  after meaningful changes.
- Run `python tools/check_manifest.py` after touching `models/`.
- Report which checks ran and their results, or explain why they could not run.
