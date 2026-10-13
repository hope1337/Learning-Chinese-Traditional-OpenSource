# Model card: paddle-ppocrv5-mobile (reference)

- **Task**: Traditional Chinese text recognition from cropped sign/menu photos.
- **Pipeline**: DB detector (quads) -> rectified line crops -> CRNN + CTC decoding.
- **Runtime**: ONNX Runtime on Android (`onnxruntime-android`), OpenCV preprocessing.
- **Training data**: upstream PaddleOCR training mix; not Taiwan-specific. Expect
  gaps on vertical text, handwriting, rare characters, and heavy glare.
- **License**: Apache-2.0 for code; verify the exact checkpoint license and
  redistribution terms before bundling any weights.
- **Provenance**: `https://github.com/PaddlePaddle/PaddleOCR`. Record checkpoint
  commit/tag and SHA-256 in `manifest.json` when adding weights by URL.

## Known limitations (to verify with `tools/eval_ocr.py`)

- Vertical lines need rotation before recognition.
- Blur and low light raise substitution/deletion errors.
- Punctuation and mixed alphanumeric strings are less stable.
- Engine confidence is not a calibrated probability.
