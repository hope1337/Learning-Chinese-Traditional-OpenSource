package com.example.chinese2.ocr

/** Stable OCR contract. The app depends on this interface only. */
enum class OcrMode { FAST, ACCURATE, AI }

data class OcrBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val text: String = "",
    val confidence: Float = 0f,
)

data class OcrDocument(
    /** Verbatim engine output. Never edited by the pipeline. */
    val rawText: String,
    val blocks: List<OcrBox> = emptyList(),
    /** Human-readable engine provenance, e.g. "ML Kit Chinese 16.0.1". */
    val engine: String,
    val elapsedMs: Long = 0L,
)

interface OcrEngine {
    suspend fun recognize(cropPng: ByteArray, mode: OcrMode): OcrDocument
}

class UnavailableOcrEngine(private val reason: String) : OcrEngine {
    override suspend fun recognize(cropPng: ByteArray, mode: OcrMode): OcrDocument =
        OcrDocument(rawText = "", engine = "Unavailable: $reason")
}
