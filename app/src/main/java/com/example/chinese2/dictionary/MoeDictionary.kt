package com.example.chinese2.dictionary

import android.content.Context
import org.json.JSONObject

/**
 * Bundled MOE reference lookup (read-only).
 * Starter ships a small seed ([entries.json]); the full MOE import replaces
 * this file on the maintainer PC without changing this API.
 */
data class DictionaryEntry(
    val headword: String,
    val pinyin: String = "",
    val meaning: String = "Not available offline yet.",
    val hanViet: String = "",
    val source: String = "MOE_TAIWAN",
)

class MoeDictionary(appContext: Context) {
    private val entries: Map<String, DictionaryEntry> = runCatching {
        val text = appContext.assets.open("dictionary/entries.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val arr = root.getJSONArray("entries")
        buildMap {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val headword = o.getString("headword")
                put(
                    headword,
                    DictionaryEntry(
                        headword = headword,
                        pinyin = o.optString("pinyin"),
                        meaning = o.optString("meaning"),
                        hanViet = o.optString("hanViet"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyMap())

    fun lookup(headword: String): DictionaryEntry =
        entries[headword] ?: DictionaryEntry(headword = headword)

    /** Longest-match segmentation, max 4 chars, for result display. */
    fun segment(text: String): List<DictionaryEntry> {
        val out = mutableListOf<DictionaryEntry>()
        var i = 0
        while (i < text.length) {
            var matched: DictionaryEntry? = null
            var len = minOf(4, text.length - i)
            while (len > 0) {
                val cand = text.substring(i, i + len)
                val e = entries[cand]
                if (e != null) {
                    matched = e
                    break
                }
                len--
            }
            if (matched != null) {
                out += matched
                i += matched.headword.length
            } else {
                val ch = text[i].toString()
                out += entries[ch] ?: DictionaryEntry(headword = ch)
                i++
            }
        }
        return out
    }

    fun size(): Int = entries.size
}
