package com.blank.gallery.domain.analyzer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.ImageDecoder
import android.net.Uri
import com.blank.gallery.data.MediaAnalysisCache
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.SimilarPhotoAnalysisResult
import com.blank.gallery.domain.SimilarPhotoGroup
import java.io.IOException
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Local pHash analysis for photos only.
 *
 * Images are decoded to a small, aspect-ratio-preserving thumbnail, normalized
 * onto a neutral square canvas, then transformed with an 8x8 low-frequency DCT.
 * Similarity groups use representative-based matching rather than connected
 * components, preventing A~B~C chains from merging dissimilar endpoints.
 */
class SimilarPhotoAnalyzer(
    context: Context,
    private val dao: MediaAssetDao
) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    suspend fun analyze(
        maxHammingDistance: Int = DEFAULT_MAX_HAMMING_DISTANCE,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): SimilarPhotoAnalysisResult = withContext(Dispatchers.IO) {
        require(maxHammingDistance in 0..MAX_HASH_BITS)
        val total = dao.countSimilarPhotoCandidates()
        val hashes = mutableListOf<Pair<MediaAsset, String>>()
        var analyzedCount = 0
        var unreadableCount = 0
        var offset = 0

        while (offset < total) {
            coroutineContext.ensureActive()
            val batch = dao.getSimilarPhotoCandidates(CANDIDATE_BATCH_SIZE, offset)
            if (batch.isEmpty()) break

            for (asset in batch) {
                coroutineContext.ensureActive()
                val cached = dao.getMediaAnalysisCache(asset.id)
                    ?.takeIf { it.matchesForPerceptualHash(asset) }
                val hash = cached?.perceptualHash ?: calculatePerceptualHash(asset.contentUri)
                if (hash == null) {
                    unreadableCount++
                } else {
                    if (cached?.perceptualHash == null) {
                        val previous = dao.getMediaAnalysisCache(asset.id)
                        dao.saveMediaAnalysisCache(
                            MediaAnalysisCache(
                                mediaAssetId = asset.id,
                                sourceSizeBytes = asset.sizeBytes,
                                sourceDateModifiedSeconds = asset.dateModifiedSeconds,
                                sha256 = previous?.sha256,
                                perceptualHash = hash,
                                hashAlgorithmVersion = previous?.hashAlgorithmVersion
                                    ?: MediaAnalysisCache.SHA256_ALGORITHM_VERSION,
                                perceptualHashAlgorithmVersion = MediaAnalysisCache.PERCEPTUAL_HASH_ALGORITHM_VERSION,
                                analyzedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    hashes += asset to hash
                }
                analyzedCount++
                onProgress(analyzedCount, total)
            }
            offset += batch.size
        }

        val groups = buildRepresentativeGroups(hashes, maxHammingDistance)
        SimilarPhotoAnalysisResult(groups, analyzedCount, unreadableCount)
    }

    private fun buildRepresentativeGroups(
        entries: List<Pair<MediaAsset, String>>,
        threshold: Int
    ): List<SimilarPhotoGroup> {
        val remaining = entries.sortedWith(
            compareByDescending<Pair<MediaAsset, String>> { it.first.dateTakenSeconds }
                .thenByDescending { it.first.dateAddedSeconds }
                .thenBy { it.first.id }
        ).toMutableList()
        val groups = mutableListOf<SimilarPhotoGroup>()

        while (remaining.isNotEmpty()) {
            val representative = remaining.removeAt(0)
            val representativeBits = representative.second.toULong(16)
            val members = mutableListOf(representative.first)
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val distance = (representativeBits xor candidate.second.toULong(16)).countOneBits()
                if (distance <= threshold) {
                    members += candidate.first
                    iterator.remove()
                }
            }
            if (members.size >= 2) {
                groups += SimilarPhotoGroup(representative.first.id, members)
            }
        }
        return groups.sortedByDescending { group -> group.items.sumOf { it.sizeBytes } }
    }

    private suspend fun calculatePerceptualHash(contentUri: String): String? {
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
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            0.299 * red + 0.587 * green + 0.114 * blue
        }

        val coefficients = DoubleArray(LOW_FREQUENCY_SIZE * LOW_FREQUENCY_SIZE)
        for (v in 0 until LOW_FREQUENCY_SIZE) {
            for (u in 0 until LOW_FREQUENCY_SIZE) {
                var sum = 0.0
                for (y in 0 until HASH_IMAGE_SIZE) {
                    val vertical = cos((2 * y + 1) * v * PI / (2.0 * HASH_IMAGE_SIZE))
                    for (x in 0 until HASH_IMAGE_SIZE) {
                        val horizontal = cos((2 * x + 1) * u * PI / (2.0 * HASH_IMAGE_SIZE))
                        sum += gray[y * HASH_IMAGE_SIZE + x] * horizontal * vertical
                    }
                }
                coefficients[v * LOW_FREQUENCY_SIZE + u] = sum
            }
        }

        val frequencies = coefficients.drop(1).sorted()
        val median = frequencies[frequencies.size / 2]
        var bits = 0uL
        var bitIndex = 0
        for (index in 1 until coefficients.size) {
            if (coefficients[index] > median) {
                bits = bits or (1uL shl bitIndex)
            }
            bitIndex++
        }
        return bits.toString(16).padStart(16, '0')
    }

    private fun MediaAnalysisCache.matchesForPerceptualHash(asset: MediaAsset): Boolean =
        sourceSizeBytes == asset.sizeBytes &&
            sourceDateModifiedSeconds == asset.dateModifiedSeconds &&
            perceptualHashAlgorithmVersion == MediaAnalysisCache.PERCEPTUAL_HASH_ALGORITHM_VERSION &&
            !perceptualHash.isNullOrBlank()

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T =
        try {
            block(this)
        } finally {
            recycle()
        }

    private companion object {
        const val HASH_IMAGE_SIZE = 32
        const val LOW_FREQUENCY_SIZE = 8
        const val MAX_HASH_BITS = 63
        const val DEFAULT_MAX_HAMMING_DISTANCE = 6
        const val CANDIDATE_BATCH_SIZE = 100
    }
}
