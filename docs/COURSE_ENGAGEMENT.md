# Course engagement guide (public)

This page maps the repository to a typical deep-learning course
open-source engagement component: a small student-created project with
genuine public interaction and a clear deep-learning connection.

## Deep-learning connection

Text recognition (OCR) is the assessed component:

- Study one open-source OCR model (default: Paddle PP-OCRv5 mobile:
  DB detector + CRNN/CTC recognizer, ONNX Runtime on Android).
- Run `tools/eval_ocr.py` on the sample crops in `models/ocr/*/eval/`.
- Document limitations honestly: vertical text, blur, glare, rare characters,
  punctuation, and uncalibrated confidence. No training from scratch is
  required; analysis of behavior on relevant images is required.

## What counts as engagement

- Reproducible issues (photo/crop + expected vs. actual + steps + version).
- Technical discussions (e.g. ML Kit vs. Paddle on the same sign).
- Documentation improvements driven by user questions.
- Code/model-card reviews with substance.

Uploading code alone is not sufficient. Keep links to issues, discussions,
and pull requests plus a short reflection: what you learned about the model
and how it relates to course concepts (detection, sequence modeling with
CTC, evaluation with CER, deployment constraints on mobile).

## Submission checklist

- [ ] Brief project overview.
- [ ] Links to public issues / discussions / PRs / review comments.
- [ ] Reflection on models/tools and course concepts.
- [ ] Note on limitations found during evaluation.
