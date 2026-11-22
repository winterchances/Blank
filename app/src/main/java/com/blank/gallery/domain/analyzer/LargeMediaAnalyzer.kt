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
        var lastSizeBytes = 0L
        var lastDateAddedSeconds = 0L
        var lastId = 0L
        var hasCursor = false

        while (true) {
            coroutineContext.ensureActive()
            val batch = dao.getLargeMediaCandidates(
                minimumSizeBytes = minimumSizeBytes,
                limit = BATCH_SIZE,
                lastSizeBytes = lastSizeBytes,
                lastDateAddedSeconds = lastDateAddedSeconds,
                lastId = lastId,
                hasCursor = hasCursor
            )
            if (batch.isEmpty()) break

            matches.addAll(batch)
            analyzedCount += batch.size
            onProgress(analyzedCount, total)
            batch.last().let { last ->
                lastSizeBytes = last.sizeBytes
                lastDateAddedSeconds = last.dateAddedSeconds
                lastId = last.id
                hasCursor = true
            }
        }

        val currentAssets = dao.revalidateAnalysisAssets(matches)
        val currentMatches = matches.mapNotNull { currentAssets[it.id] }
        LargeMediaAnalysisResult(
            minimumSizeBytes = minimumSizeBytes,
            items = currentMatches,
            analyzedCount = analyzedCount,
            totalSizeBytes = currentMatches.sumOf { it.sizeBytes },
            photoCount = currentMatches.count { it.mediaType == MEDIA_TYPE_IMAGE },
            videoCount = currentMatches.count { it.mediaType == MEDIA_TYPE_VIDEO }
        )
    }

    private companion object {
        const val BATCH_SIZE = 200
        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2
        const val DEFAULT_MINIMUM_SIZE_BYTES = 100L * 1024L * 1024L
    }
}
