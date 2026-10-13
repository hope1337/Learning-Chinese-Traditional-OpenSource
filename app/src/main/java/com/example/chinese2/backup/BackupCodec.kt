package com.example.chinese2.backup

import android.content.Context
import android.net.Uri
import com.example.chinese2.storage.Flashcard
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupPayload(
    val cards: List<Flashcard>,
    val history: List<String>,
    val exportedAt: Long = System.currentTimeMillis(),
)

object BackupCodec {
    fun export(context: Context, uri: Uri, payload: BackupPayload) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { raw ->
            ZipOutputStream(raw).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                val manifest = JSONObject()
                    .put("format", "chinese2-backup")
                    .put("schemaVersion", 1)
                    .put("exportedAt", payload.exportedAt)
                    .put("cardCount", payload.cards.size)
                    .toString(2)
                zip.write(manifest.toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("data/cards.jsonl"))
                val cards = payload.cards.joinToString("\n") { "${it.id}\t${it.text}\t${it.meaning}\t${it.dueAt}\t${it.intervalDays}" }
                zip.write(cards.toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("data/history.jsonl"))
                zip.write(payload.history.joinToString("\n").toByteArray())
                zip.closeEntry()
            }
        }
    }

    fun parse(context: Context, uri: Uri): BackupPayload {
        val cards = mutableListOf<Flashcard>()
        val history = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val bytes = zip.readBytes()
                    when (entry.name) {
                        "data/cards.jsonl" -> bytes.toString(Charsets.UTF_8).lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            runCatching {
                                val p = line.split("\t")
                                cards += Flashcard(
                                    id = p[0], text = p.getOrElse(1) { "" },
                                    meaning = p.getOrElse(2) { "" },
                                    dueAt = p.getOrElse(3) { "0" }.toLong(),
                                    intervalDays = p.getOrElse(4) { "0" }.toInt(),
                                )
                            }
                        }
                        "data/history.jsonl" -> bytes.toString(Charsets.UTF_8).lines().forEach {
                            if (it.isNotBlank()) history += it
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return BackupPayload(cards = cards, history = history)
    }

    @Suppress("unused")
    fun cardJson(cards: List<Flashcard>): JSONArray {
        val arr = JSONArray()
        cards.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text)) }
        return arr
    }
}
