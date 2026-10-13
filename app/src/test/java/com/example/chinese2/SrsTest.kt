package com.example.chinese2

import com.example.chinese2.storage.Flashcard
import com.example.chinese2.storage.UserDataRepository
import com.example.chinese2.storage.UserDataRepository.ReviewGrade
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SrsTest {
    @Test
    fun againResetsInterval() {
        val dir = Files.createTempDirectory("srs").toFile()
        val repo = UserDataRepository(File(dir, "root"))
        val card = Flashcard(id = "dictionary:車", text = "車", meaning = "vehicle")
        val reviewed = repo.review(card, ReviewGrade.AGAIN)
        assertEquals(0, reviewed.intervalDays)
        assertTrue(reviewed.dueAt > System.currentTimeMillis())
    }

    @Test
    fun goodDoublesInterval() {
        val dir = Files.createTempDirectory("srs").toFile()
        val repo = UserDataRepository(File(dir, "root"))
        val card = Flashcard(id = "x", text = "x", intervalDays = 2, dueAt = 0)
        val reviewed = repo.review(card, ReviewGrade.GOOD)
        assertEquals(4, reviewed.intervalDays)
    }

    @Test
    fun easyCapsAt365() {
        val dir = Files.createTempDirectory("srs").toFile()
        val repo = UserDataRepository(File(dir, "root"))
        val card = Flashcard(id = "x", text = "x", intervalDays = 300, dueAt = 0)
        val reviewed = repo.review(card, ReviewGrade.EASY)
        assertEquals(365, reviewed.intervalDays)
    }
}
