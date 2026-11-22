package com.blank.gallery.domain.analyzer

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.LargeMediaAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Finds large local photos and videos using indexed MediaStore metadata.
 * It does not decode, hash, move, or delete any media.
 */
class LargeMediaAnalyzer(
    private val dao: MediaAssetDao
) {
    suspend fun analyze(
        minimumSizeBytes: Long = DEFAULT_MINIMUM_SIZE_BYTES,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): LargeMediaAnalysisResult = withContext(Dispatchers.IO) {
        require(minimumSizeBytes > 0L) { "Minimum file size must be positive." }

        val total = dao.countLargeMediaCandidates(minimumSizeBytes)
        val matches = mutableListOf<MediaAsset>()
        var analyzedCount = 0
        var offset = 0

        while (offset < total) {
            coroutineContext.ensureActive()
            val batch = dao.getLargeMediaCandidates(
                minimumSizeBytes = minimumSizeBytes,
                limit = BATCH_SIZE,
                offset = offset
            )
            if (batch.isEmpty()) break

            matches.addAll(batch)
            analyzedCount += batch.size
            onProgress(analyzedCount, total)
            offset += batch.size
        }

        LargeMediaAnalysisResult(
            minimumSizeBytes = minimumSizeBytes,
            items = matches,
            analyzedCount = analyzedCount,
            totalSizeBytes = matches.sumOf { it.sizeBytes },
            photoCount = matches.count { it.mediaType == MEDIA_TYPE_IMAGE },
            videoCount = matches.count { it.mediaType == MEDIA_TYPE_VIDEO }
        )
    }

    private companion object {
        const val BATCH_SIZE = 200
        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2
        const val DEFAULT_MINIMUM_SIZE_BYTES = 100L * 1024L * 1024L
    }
}
