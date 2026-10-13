package com.example.chinese2.ocr

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

/** Bundled fast path: ML Kit Chinese text recognition, on-device. */
class MlKitOcrEngine(appContext: Context) : OcrEngine {
    private val recognizer =
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override suspend fun recognize(cropPng: ByteArray, mode: OcrMode): OcrDocument {
        val started = SystemClock.elapsedRealtime()
        val bitmap = BitmapFactory.decodeByteArray(cropPng, 0, cropPng.size)
            ?: return OcrDocument(rawText = "", engine = "ML Kit Chinese 16.0.1 (decode failed)")
        val result: Text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
        val blocks = result.textBlocks.flatMap { block ->
            block.lines.flatMap { line ->
                line.elements.map { element ->
                    val box = element.boundingBox
                    OcrBox(
                        left = (box?.left ?: 0).toFloat(),
                        top = (box?.top ?: 0).toFloat(),
                        right = (box?.right ?: 0).toFloat(),
                        bottom = (box?.bottom ?: 0).toFloat(),
                        text = element.text,
                    )
                }
            }
        }
        return OcrDocument(
            rawText = result.text,
            blocks = blocks,
            engine = "ML Kit Chinese 16.0.1",
            elapsedMs = SystemClock.elapsedRealtime() - started,
        )
    }
}
