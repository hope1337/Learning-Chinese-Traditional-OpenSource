package com.example.chinese2.strokes

/**
 * MOE stroke-order geometry for one character.
 * Converted technically from official MOE XML; shapes and order unchanged.
 */
data class StrokeOrder(
    val character: String,
    val strokeCount: Int,
    val viewBox: Int = 100,
    val strokes: List<String> = emptyList(),
    val source: String = "MOE_TAIWAN",
    val license: String = "CC BY-NC-ND 3.0 Taiwan",
)
