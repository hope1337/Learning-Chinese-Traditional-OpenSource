package com.example.chinese2.strokes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Stroke-order viewer. With bundled MOE data it steps through real strokes;
 * without data it shows a quiet unavailable state (never a mock fallback).
 */
@Composable
fun StrokeViewer(glyph: String, repository: StrokeRepository) {
    var data by remember(glyph) { mutableStateOf<StrokeData?>(null) }
    var loaded by remember(glyph) { mutableStateOf(false) }
    var current by remember(glyph) { mutableIntStateOf(1) }
    var playing by remember(glyph) { mutableStateOf(false) }

    LaunchedEffect(glyph) {
        data = repository.load(glyph)
        loaded = true
        current = 1
        playing = false
    }
    LaunchedEffect(playing, data) {
        val total = data?.strokes?.size ?: 0
        while (playing && current < total) {
            delay(700)
            current++
        }
        if (data != null && current >= total) playing = false
    }

    Column {
        Text("Stroke order")
        if (!loaded) {
            Text("Loading…")
            return
        }
        val d = data
        if (d == null) {
            Text("Not available offline yet for “$glyph”.")
            Text(
                "Source: Ministry of Education (Taiwan) · CC BY-NC-ND 3.0 TW. " +
                    "Run tools/fetch_moe.py on the maintainer PC to bundle it.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            return
        }
        val total = d.strokes.size
        Text("Stroke $current / $total · MOE Taiwan")
        LinearProgressIndicator(
            progress = { current.toFloat() / total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(d.strokes.getOrNull(current - 1) ?: "", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Row {
            OutlinedButton(onClick = { current = maxOf(1, current - 1) }) { Text("Prev") }
            OutlinedButton(onClick = { current = minOf(total, current + 1) }) { Text("Next") }
            OutlinedButton(onClick = { current = 1; playing = true }) { Text("Replay") }
            OutlinedButton(onClick = { playing = !playing }) { Text(if (playing) "Pause" else "Play") }
        }
        Text("Source: Ministry of Education (Taiwan)")
    }
}
