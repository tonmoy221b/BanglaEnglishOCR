package com.example.banglaocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

/**
 * Tesseract wrapper for Bangla + English.
 * Needs assets/tessdata/ben.traineddata and eng.traineddata.
 */
object OcrEngine {

    private const val LANGS = "ben+eng"
    private const val MIN_LINE_CONFIDENCE = 55   // lines below this are treated as pictures/noise
    private const val MAX_SIDE = 2200            // downscale huge photos for speed

    private val lock = Any()
    private var tess: TessBaseAPI? = null

    /** Call from a background thread. Copies language files on first run. */
    fun init(ctx: Context): Boolean {
        synchronized(lock) {
            if (tess != null) return true
            val dir = File(ctx.filesDir, "tessdata").apply { mkdirs() }
            for (name in listOf("eng.traineddata", "ben.traineddata")) {
                val f = File(dir, name)
                if (!f.exists() || f.length() == 0L) {
                    ctx.assets.open("tessdata/$name").use { input ->
                        f.outputStream().use { out -> input.copyTo(out) }
                    }
                }
            }
            val api = TessBaseAPI()
            if (!api.init(ctx.filesDir.absolutePath, LANGS, TessBaseAPI.OEM_LSTM_ONLY)) {
                api.recycle()
                return false
            }
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            tess = api
            return true
        }
    }

    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /** Grayscale + mild contrast boost + size limit. */
    private fun preprocess(src: Bitmap): Bitmap {
        val scale = MAX_SIDE.toFloat() / maxOf(src.width, src.height)
        val w = if (scale < 1f) (src.width * scale).toInt() else src.width
        val h = if (scale < 1f) (src.height * scale).toInt() else src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val c = 1.3f
        val t = (-0.5f * c + 0.5f) * 255f
        cm.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f)))
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
        Canvas(out).drawBitmap(src, null, Rect(0, 0, w, h), paint)
        return out
    }

    /** Returns recognized text only (picture regions / low-confidence noise skipped). */
    fun recognize(bitmap: Bitmap): String {
        synchronized(lock) {
            val api = tess ?: return ""
            val img = preprocess(bitmap)
            api.setImage(img)
            api.utf8Text // triggers recognition
            val sb = StringBuilder()
            val it = api.resultIterator
            if (it != null) {
                it.begin()
                do {
                    val line = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)?.trim().orEmpty()
                    val conf = it.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)
                    if (line.isNotEmpty() && conf >= MIN_LINE_CONFIDENCE && hasRealLetters(line)) {
                        sb.append(line).append('\n')
                    }
                } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE))
                it.delete()
            }
            api.clear()
            img.recycle()
            return sb.toString().trim()
        }
    }

    // Drops lines made only of symbols (typical output for photos/graphics).
    private fun hasRealLetters(s: String) = s.count { it.isLetterOrDigit() } >= 2
}
