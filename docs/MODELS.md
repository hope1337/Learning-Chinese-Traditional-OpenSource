# OCR models: contract and evaluation

The deep-learning contribution surface is `models/`. Contributors work in
Python on a PC; the app consumes results through one Kotlin interface.

## Contract

```kotlin
interface OcrEngine {
    suspend fun recognize(image: ByteArray, mode: OcrMode): OcrDocument
}

data class OcrDocument(
    val rawText: String,      // engine output, unedited
    val blocks: List<OcrBlock>, // spatial boxes, normalized coordinates
    val engine: String,       // e.g. "ML Kit Chinese 16.0.1", "PP-OCRv5 mobile / ONNX"
    val elapsedMs: Long,
)
```

- `OcrMode = { FAST, ACCURATE, AI }`. `FAST` is bundled ML Kit;
  `ACCURATE` routes to the active `models/ocr/*` ONNX entry;
  `AI` is an optional vision path and never silently replaces OCR text.
- Confidence scores are engine scores, not calibrated probabilities.
- Traditional Chinese is preserved exactly. No Simplified conversion.

## Reference architecture (what contributors study)

- **Detection**: DB (Differentiable Binarization) text detector producing
  quads at up to 1280px on the long side.
- **Recognition**: CRNN with CTC decoding per rectified line, including
  vertical-line rotation before recognition.
- **What to report**: character error rate (CER) on Taiwan street-sign and
  menu crops, plus failure buckets: vertical text, blur, low light,
  glare, occlusions, rare characters, punctuation.

## Model folder layout

```
models/ocr/<model-id>/
  manifest.json   # id, role=ocr, displayName, license, sha256, thresholds
  model-card.md   # architecture, training data, limits, provenance
  EVAL.md         # what was tested and what failed
  eval/images/*.png + expected.txt
```

Validate with `python tools/check_manifest.py` and score with
`python tools/eval_ocr.py --model <model-id>`. Paste the output in the PR.
