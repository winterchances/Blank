package com.blank.gallery.domain.analyzer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.IOException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Shared local pHash implementation used by similarity and burst analysis.
 * The image is decoded to a small thumbnail before any pixel processing.
 */
internal class PerceptualHashCalculator(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val cosine = Array(LOW_FREQUENCY_SIZE) { frequency ->
        DoubleArray(HASH_IMAGE_SIZE) { position ->
            cos((2 * position + 1) * frequency * PI / (2.0 * HASH_IMAGE_SIZE))
        }
    }

    suspend fun calculate(contentUri: String): String? {
        coroutineContext.ensureActive()
        return try {
            val source = ImageDecoder.createSource(resolver, Uri.parse(contentUri))
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = min(
                    HASH_IMAGE_SIZE.toFloat() / info.size.width.coerceAtLeast(1),
                    HASH_IMAGE_SIZE.toFloat() / info.size.height.coerceAtLeast(1)
                )
                decoder.setTargetSize(
                    max(1, (info.size.width * scale).toInt()),
                    max(1, (info.size.height * scale).toInt())
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            decoded.useBitmap { sourceBitmap ->
                val normalized = Bitmap.createBitmap(
                    HASH_IMAGE_SIZE,
                    HASH_IMAGE_SIZE,
                    Bitmap.Config.ARGB_8888
                )
                try {
                    val canvas = Canvas(normalized)
                    canvas.drawColor(Color.rgb(128, 128, 128))
                    val scale = min(
                        HASH_IMAGE_SIZE.toFloat() / sourceBitmap.width,
                        HASH_IMAGE_SIZE.toFloat() / sourceBitmap.height
                    )
                    val width = sourceBitmap.width * scale
                    val height = sourceBitmap.height * scale
                    val left = (HASH_IMAGE_SIZE - width) / 2f
                    val top = (HASH_IMAGE_SIZE - height) / 2f
                    canvas.drawBitmap(
                        sourceBitmap,
                        null,
                        RectF(left, top, left + width, top + height),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    createHash(normalized)
                } finally {
                    normalized.recycle()
                }
            }
        } catch (error: IOException) {
            null
        } catch (error: SecurityException) {
            null
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    private fun createHash(bitmap: Bitmap): String {
        val pixels = IntArray(HASH_IMAGE_SIZE * HASH_IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, HASH_IMAGE_SIZE, 0, 0, HASH_IMAGE_SIZE, HASH_IMAGE_SIZE)
        val gray = DoubleArray(pixels.size) { index ->
            val color = pixels[index]
            0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)
        }
        val coefficients = DoubleArray(LOW_FREQUENCY_SIZE * LOW_FREQUENCY_SIZE)
        for (v in 0 until LOW_FREQUENCY_SIZE) {
            for (u in 0 until LOW_FREQUENCY_SIZE) {
                var sum = 0.0
                for (y in 0 until HASH_IMAGE_SIZE) {
                    val vertical = cosine[v][y]
                    for (x in 0 until HASH_IMAGE_SIZE) {
                        sum += gray[y * HASH_IMAGE_SIZE + x] * cosine[u][x] * vertical
                    }
                }
                coefficients[v * LOW_FREQUENCY_SIZE + u] = sum
            }
        }
        val median = coefficients.drop(1).sorted()[coefficients.size / 2]
        var bits = 0uL
        var bitIndex = 0
        for (index in 1 until coefficients.size) {
            if (coefficients[index] > median) bits = bits or (1uL shl bitIndex)
            bitIndex++
        }
        return bits.toString(16).padStart(16, '0')
    }

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T =
        try {
            block(this)
        } finally {
            recycle()
        }

    private companion object {
        const val HASH_IMAGE_SIZE = 32
        const val LOW_FREQUENCY_SIZE = 8
    }
}
