package com.example.chinese2.ocr

/**
 * ONNX path for model contributors (see models/ocr folder).
 *
 * The adapter resolves the active model id from `models/catalog.json`
 * (mirrored into assets at release time) and runs detection + recognition
 * with ONNX Runtime. Model contributors never touch this file: they only
 * add a folder under `models/ocr/<model-id>/` and score it with
 * `tools/eval_ocr.py`.
 *
 * This starter implementation reports "not installed" until weights for the
 * active model are present under `noBackupFilesDir/models/ocr/<id>/`.
 */
class OnnxOcrEngine(
    private val activeModelId: () -> String?,
    private val weightsPresent: (String) -> Boolean,
) : OcrEngine {
    override suspend fun recognize(cropPng: ByteArray, mode: OcrMode): OcrDocument {
        val id = activeModelId() ?: return OcrDocument(
            rawText = "",
            engine = "ONNX OCR: no active model selected",
        )
        if (!weightsPresent(id)) {
            return OcrDocument(rawText = "", engine = "ONNX OCR ($id): weights not installed")
        }
        // Full DB + CRNN wiring lands here once the first contributed model
        // passes `tools/eval_ocr.py` quality gates and license review.
        return OcrDocument(rawText = "", engine = "ONNX OCR ($id): runtime not wired yet")
    }
}
