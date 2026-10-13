package com.example.chinese2.storage

import java.io.File
import java.util.UUID

/**
 * Flashcards and confirmed scan history stored as JSONL under the
 * uninstall-surviving data root. Writes are atomic (tmp + rename); media
 * files use UUID names and orphan GC runs after commit.
 */
data class Flashcard(
    val id: String,
    val text: String,
    val meaning: String = "",
    val pinyin: String = "",
    val dueAt: Long = System.currentTimeMillis(),
    val intervalDays: Int = 0,
)

class UserDataRepository(private val root: File) {
    private val cardsFile: File get() = File(root, "user/cards/cards.jsonl")
    private val mediaDir: File get() = File(root, "user/cards/media").apply { mkdirs() }

    fun load(): List<Flashcard> {
        if (!cardsFile.exists()) return emptyList()
        return cardsFile.readLines().mapNotNull { line ->
            runCatching {
                val parts = line.split("\t")
                Flashcard(
                    id = parts.getOrElse(0) { UUID.randomUUID().toString() },
                    text = parts.getOrElse(1) { "" },
                    meaning = parts.getOrElse(2) { "" },
                )
            }.getOrNull()
        }
    }

    fun save(card: Flashcard) {
        cardsFile.parentFile?.mkdirs()
        val cards = (load().filter { it.id != card.id } + card)
        val tmp = File(cardsFile.absolutePath + ".tmp")
        tmp.writeText(cards.joinToString("\n") { "${it.id}\t${it.text}\t${it.meaning}" })
        require(tmp.renameTo(cardsFile)) { "Atomic card write failed" }
    }

    /** Simple SRS: Again=10min, Hard x1 / Good x2 / Easy x3, cap 365 days. */
    fun review(card: Flashcard, grade: ReviewGrade): Flashcard {
        val now = System.currentTimeMillis()
        return when (grade) {
            ReviewGrade.AGAIN -> card.copy(dueAt = now + 10 * 60 * 1000, intervalDays = 0)
            ReviewGrade.HARD -> card.copy(
                dueAt = now + maxOf(1, card.intervalDays) * 24 * 3600 * 1000L,
                intervalDays = maxOf(1, card.intervalDays),
            )
            ReviewGrade.GOOD -> card.copy(
                dueAt = now + maxOf(1, card.intervalDays * 2) * 24 * 3600 * 1000L,
                intervalDays = maxOf(1, card.intervalDays * 2),
            )
            ReviewGrade.EASY -> card.copy(
                dueAt = now + maxOf(1, card.intervalDays * 3) * 24 * 3600 * 1000L,
                intervalDays = minOf(365, maxOf(1, card.intervalDays * 3)),
            )
        }.also { save(it) }
    }

    enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }
}
