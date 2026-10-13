# Contributing models without Android

You only need Python 3.10+ and `pip install -r tools/requirements.txt`.
No Android Studio, no phone.

## Add or improve an OCR model

1. Copy `models/ocr/paddle-ppocrv5-mobile/` to `models/ocr/<your-model-id>/`.
2. Edit `manifest.json`: id, display name, license, source URL, SHA-256,
   input size, and minimum quality thresholds.
3. Write `model-card.md`: architecture (detector + recognizer + decoder),
   training data, languages/scripts, known limits on Traditional Chinese.
4. Add test crops to `eval/images/` (your own photos or synthetic crops you
   have rights to share) and the exact expected text in `eval/expected.txt`.
5. Run and paste the output in your PR:

```powershell
python tools/check_manifest.py
python tools/eval_ocr.py --model <your-model-id>
```

## Rules

- Preserve Traditional Chinese exactly in `expected.txt`. Do not convert to
  Simplified.
- Keep `engine` provenance honest: report the real model name and version.
- Confidence values are engine scores, not calibrated probabilities.
- Never commit `.onnx`, `.bin`, `.gguf`, or MOE bytes. Link weights by URL +
  SHA-256; bundling happens at release time after license review.
- One model per pull request. Small, reviewable changes only.

## How model folders reach the app

The app ships an `OnnxOcrEngine` adapter that loads the active
`models/ocr/*` entry from `models/catalog.json`. App maintainers wire the
adapter; model contributors never touch Kotlin.
