# Contributing to Chinese2

Thank you for your interest. This project is designed so that **model
contributors never need Android Studio**: everything about OCR models can be
done in Python on a PC. App contributors use Android Studio.

## Ways to contribute

1. **Model contributions (no Android needed)** — see
   `models/CONTRIBUTING-MODELS.md`. Add or improve an OCR model folder under
   `models/ocr/<your-model>/`, run `tools/eval_ocr.py`, open a pull request
   with the evaluation output.
2. **App contributions** — Kotlin + Compose under `app/src/main/java/`.
   Follow `AGENTS.md`. Small, incremental pull requests only.
3. **Documentation and reproducible issues** — well-documented issues with
   photos (or synthetic crops), expected vs. actual text, steps, and app
   version are first-class contributions.

## Ground rules

- English only in code, docs, issues, and pull requests. Traditional Chinese
  text under test is data and must be quoted exactly.
- Taiwan Traditional Chinese first. Do not convert test data to Simplified
  Chinese silently.
- Respect licenses: app code is MIT; MOE reference data is CC BY-NC-ND 3.0
  Taiwan (non-commercial, no derivatives, attribution). Never commit MOE bytes
  or large weights to git. See `licenses/MOE_NOTE.md`.
- No backend, no account, no analytics. Do not add network calls without
  explicit maintainer approval.

## Pull request checklist

- [ ] English description of what changed and why.
- [ ] For models: `manifest.json` valid (`python tools/check_manifest.py`),
      `model-card.md` + `EVAL.md` updated, eval output pasted.
- [ ] For app: `.\gradlew.bat :app:assembleDebug` result reported.
- [ ] No MOE bytes, weights, or user photos committed (samples only with consent).
- [ ] Provenance (source URL, license, SHA-256) recorded for any new dataset.

## Good first issues

Look for labels `good-first-issue` and `ocr-eval`: typically one failing
street-sign or menu photo with a known expected transcription.
