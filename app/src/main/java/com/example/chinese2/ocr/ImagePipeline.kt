package com.example.chinese2.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/** Normalized crop region (0..1). Default is the full image. */
data class CropRegion(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    fun width(): Float = (right - left).coerceIn(0.05f, 1f)
    fun height(): Float = (bottom - top).coerceIn(0.05f, 1f)
}

/**
 * Minimal image pipeline: decode with a size bound, apply EXIF orientation
 * plus user quarter-turns, then crop the normalized ROI and encode PNG.
 * Preview bitmaps are downscaled; OCR bytes use up to 2400px on the long side.
 */
object ImagePipeline {
    fun loadPreview(file: File, maxSide: Int = 1400): Bitmap? =
        decodeBounded(file, maxSide)?.let { applyExif(it, file, 0) }

    fun cropForOcr(file: File, region: CropRegion, quarterTurns: Int): ByteArray? {
        val decoded = decodeBounded(file, 2400) ?: return null
        val oriented = applyExif(decoded, file, quarterTurns)
        val w = oriented.width
        val h = oriented.height
        val l = (region.left * w).toInt().coerceIn(0, w - 1)
        val t = (region.top * h).toInt().coerceIn(0, h - 1)
        val r = (region.right * w).toInt().coerceIn(l + 1, w)
        val b = (region.bottom * h).toInt().coerceIn(t + 1, h)
        val cropped = Bitmap.createBitmap(oriented, l, t, r - l, b - t)
        val out = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
        if (cropped !== oriented) cropped.recycle()
        if (oriented !== decoded) oriented.recycle()
        return out.toByteArray()
    }

    private fun decodeBounded(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while ((bounds.outWidth / sample) > maxSide || (bounds.outHeight / sample) > maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun applyExif(bitmap: Bitmap, file: File, quarterTurns: Int): Bitmap {
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
        ) ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        }
        if (quarterTurns % 4 != 0) matrix.postRotate((quarterTurns * 90).toFloat())
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun copyToCache(context: Context, source: android.net.Uri, name: String): File? = runCatching {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val out = File(dir, name)
        context.contentResolver.openInputStream(source)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        out
    }.getOrNull()
}
