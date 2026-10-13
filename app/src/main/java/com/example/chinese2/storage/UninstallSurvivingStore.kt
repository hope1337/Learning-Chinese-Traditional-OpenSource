package com.example.chinese2.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Cards + history persistence that prefers the user-selected SAF folder
 * (survives uninstall) and falls back to app-internal storage.
 */
class UninstallSurvivingStore(
    private val context: Context,
    private val appDataRoot: AppDataRoot,
) {
    private fun externalDir(): DocumentFile? {
        val tree = appDataRoot.treeUri() ?: return null
        var dir = DocumentFile.fromTreeUri(context, tree) ?: return null
        for (seg in listOf("user", "cards")) {
            dir = dir.findFile(seg)?.takeIf { it.isDirectory }
                ?: dir.createDirectory(seg) ?: return null
        }
        return dir
    }

    private fun internalRoot(): File =
        File(context.filesDir, "app-data-root").apply { mkdirs() }

    fun loadCards(): List<Flashcard> {
        val repo = UserDataRepository(if (appDataRoot.hasExternalRoot()) internalRoot() else internalRoot())
        // External path first; fall back to internal file.
        externalDir()?.let { dir ->
            val text = readDoc(dir, "cards.jsonl") ?: return@let
            return text.lines().mapNotNull { line ->
                if (line.isBlank()) null else runCatching {
                    val p = line.split("\t")
                    Flashcard(id = p.getOrElse(0) { "" }, text = p.getOrElse(1) { "" },
                        meaning = p.getOrElse(2) { "" })
                }.getOrNull()
            }
        }
        return repo.load()
    }

    fun saveCard(card: Flashcard) {
        val all = loadCards().filter { it.id != card.id } + card
        val payload = all.joinToString("\n") { "${it.id}\t${it.text}\t${it.meaning}" }
        val dir = externalDir()
        if (dir != null) {
            writeDoc(dir, "cards.jsonl", payload)
        } else {
            UserDataRepository(internalRoot()).save(card)
            // Rewrite full list for consistency.
            val f = File(internalRoot(), "user/cards/cards.jsonl")
            f.parentFile?.mkdirs()
            val tmp = File(f.absolutePath + ".tmp")
            tmp.writeText(payload)
            tmp.renameTo(f)
        }
    }

    fun deleteCard(id: String) {
        val all = loadCards().filter { it.id != id }
        val payload = all.joinToString("\n") { "${it.id}\t${it.text}\t${it.meaning}" }
        externalDir()?.let { writeDoc(it, "cards.jsonl", payload) }
            ?: run {
                val f = File(internalRoot(), "user/cards/cards.jsonl")
                f.parentFile?.mkdirs()
                f.writeText(payload)
            }
    }

    fun deleteAll() {
        externalDir()?.let {
            it.findFile("cards.jsonl")?.delete()
            it.findFile("cards.jsonl.tmp")?.delete()
        }
        File(internalRoot(), "user/cards/cards.jsonl").delete()
        File(context.filesDir, "app-data-root/scan-history/history.jsonl").delete()
        File(context.cacheDir, "photos").deleteRecursively()
    }

    private fun readDoc(dir: DocumentFile, name: String): String? = runCatching {
        val f = dir.findFile(name) ?: return null
        context.contentResolver.openInputStream(f.uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    private fun writeDoc(dir: DocumentFile, name: String, text: String) {
        var f = dir.findFile(name)
        if (f == null || !f.isFile) {
            f?.delete()
            f = dir.createFile("application/json", name)
        }
        val uri: Uri = f?.uri ?: return
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(text)
        }
    }
}
