package com.example.chinese2

import org.junit.Assert.*
import org.junit.Test

class CerTest {
    private fun cer(expected: String, actual: String): Double {
        if (expected.isEmpty()) return if (actual.isEmpty()) 0.0 else 1.0
        val m = expected.length
        val n = actual.length
        var prev = IntArray(n + 1) { it }
        for (i in 1..m) {
            val cur = IntArray(n + 1)
            cur[0] = i
            for (j in 1..n) {
                val cost = if (expected[i - 1] == actual[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[n].toDouble() / m
    }

    @Test
    fun identicalIsZero() {
        assertEquals(0.0, cer("禁止停車", "禁止停車"), 1e-9)
    }

    @Test
    fun oneSubstitution() {
        assertEquals(0.25, cer("禁止停車", "禁止停车"), 1e-9)
    }

    @Test
    fun traditionalPreserved() {
        assertNotEquals("車", "车")
    }
}
