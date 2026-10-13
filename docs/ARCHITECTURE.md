# Architecture

Minimal offline-first scope for `Chinese2`.

```
app/src/main/java/com/example/chinese2/
  MainActivity.kt        # Composition root, single-activity navigation
  ui/Screens.kt          # Home / ROI / Review / Result / Character (Compose)
  ocr/OcrContract.kt     # OcrEngine interface, OcrDocument, OcrMode
  ocr/MlKitOcrEngine.kt  # Bundled ML Kit Chinese path (fast)
  ocr/OnnxOcrEngine.kt   # ONNX path for models/* contributors (accurate)
  dictionary/MoeDictionary.kt  # Bundled MOE reference lookup (read-only)
  strokes/StrokeOrder.kt       # MOE stroke geometry + player state
  storage/AppDataRoot.kt       # User-selected SAF folder (uninstall-surviving)
  storage/UserDataRepository.kt# Flashcards + history in the SAF folder
  models/ModelManager.kt       # Active-model selection, on-demand weights

app/src/main/assets/
  dictionary/manifest.json   # placeholder; real DB generated on maintainer PC
  stroke_order/index.json    # placeholder; real JSON generated on maintainer PC

models/                    # PC-first model contribution surface (no Android needed)
  catalog.json
  ocr/<model-id>/          # manifest.json + model-card.md + EVAL.md + eval/
  translation/<model-id>/

tools/                     # PC-only scripts
  fetch_moe.py             # download MOE originals with provenance
  check_manifest.py        # validate models/catalog.json + manifests
  eval_ocr.py              # score an OCR model folder on sample crops
```

## Key flows

1. **Capture**: camera (`TakePicture` full-URI) or gallery (Photo Picker)
   lands in `cacheDir/photos/` staging.
2. **ROI**: shared selection screen, default full image. Crops map to
   original-resolution pixels with EXIF + user rotation applied
   (`BitmapRegionDecoder`, lossless PNG via `FileProvider`).
3. **Recognize**: `OcrEngine.recognize(crop)` returns `OcrDocument`
   (raw text + blocks + engine name + elapsed). Engines run serialized,
   lazily, off the main thread.
4. **Review**: editable confirmation screen. Only confirmed text flows to
   dictionary segmentation and sentence handling.
5. **Learn**: dictionary lookup (MOE packs first, bundled fallback), character
   detail (meanings, stroke animation, examples), flashcard save with simple
   SRS (`Again=10min`, `Hard x1 / Good x2 / Easy x3`, cap 365 days).

## Non-goals

No backend, no account, no analytics, no cloud sync. No Room/DataStore:
persistence is JSONL + atomic file writes with validate-first restore.
