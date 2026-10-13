package com.example.chinese2.strokes

import android.content.Context
import org.json.JSONObject

data class StrokeData(
    val character: String,
    val strokes: List<String>,
    val source: String = "MOE_TAIWAN",
)

/** Loads bundled MOE stroke JSON `assets/stroke_order/U+XXXX.json` when present. */
class StrokeRepository(private val appContext: Context) {
    fun load(glyph: String): StrokeData? {
        if (glyph.codePointCount(0, glyph.length) != 1) return null
        val code = glyph.codePointAt(0)
        val name = "stroke_order/U+%04X.json".format(code)
        return runCatching {
            val text = appContext.assets.open(name).bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            if (obj.optString("source") != "MOE_TAIWAN") return null
            val arr = obj.optJSONArray("strokes") ?: return null
            val strokes = List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
            if (strokes.isEmpty()) return null
            StrokeData(character = glyph, strokes = strokes)
        }.getOrNull()
    }
}
