package com.example.chinese2.storage

import java.io.File

data class HistoryEntry(val id: String, val text: String, val time: Long = System.currentTimeMillis())

/** Confirmed scan history, capped at 100, JSONL tab-separated. */
class HistoryStore(root: File) {
    private val file: File = File(root, "scan-history/history.jsonl")

    fun load(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            runCatching {
                val p = line.split("\t")
                HistoryEntry(id = p[0], text = p[1], time = p.getOrElse(2) { "0" }.toLong())
            }.getOrNull()
        }.sortedByDescending { it.time }.take(100)
    }

    fun add(text: String) {
        val entry = HistoryEntry(id = java.util.UUID.randomUUID().toString(), text = text)
        val all = (listOf(entry) + load()).take(100)
        file.parentFile?.mkdirs()
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(all.joinToString("\n") { "${it.id}\t${it.text}\t${it.time}" })
        tmp.renameTo(file)
    }

    fun clear() {
        file.delete()
    }
}
