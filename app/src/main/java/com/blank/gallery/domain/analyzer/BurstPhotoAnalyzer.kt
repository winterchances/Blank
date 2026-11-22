package com.blank.gallery.domain.analyzer

import android.content.Context
import com.blank.gallery.data.MediaAnalysisCache
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.BurstPhotoAnalysisResult
import com.blank.gallery.domain.BurstPhotoGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Detects burst candidates using capture-time proximity AND visual similarity.
 * It never infers a burst from timestamps alone.
 */
class BurstPhotoAnalyzer(
    context: Context,
    private val dao: MediaAssetDao
) {
    private val hashCalculator = PerceptualHashCalculator(context)

    suspend fun analyze(
        maxAdjacentGapMillis: Long = DEFAULT_MAX_ADJACENT_GAP_MILLIS,
        maxBurstDurationMillis: Long = DEFAULT_MAX_BURST_DURATION_MILLIS,
        maxRepresentativeHammingDistance: Int = DEFAULT_MAX_HAMMING_DISTANCE,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BurstPhotoAnalysisResult = withContext(Dispatchers.IO) {
        require(maxAdjacentGapMillis in 1L..MAX_BURST_DURATION_MILLIS)
        require(maxBurstDurationMillis in maxAdjacentGapMillis..MAX_BURST_DURATION_MILLIS)
        require(maxRepresentativeHammingDistance in 0..MAX_HASH_BITS)

        val maxMediaAssetId = dao.getMaxMediaAssetId()
        val total = dao.countBurstPhotoCandidates(maxMediaAssetId)
        val groups = mutableListOf<BurstPhotoGroup>()
        var currentItems = mutableListOf<MediaAsset>()
        var currentHashes = mutableListOf<ULong>()
        var currentStartMillis = 0L
        var previousCaptureMillis = 0L
        var currentMaxAdjacentGap = 0L
        var currentMaxRepresentativeDistance = 0
        var analyzedCount = 0
        var unreadableCount = 0
        var lastDateTakenMillis = 0L
        var lastId = 0L
        var hasCursor = false

        fun finishCurrentGroup() {
            if (currentItems.size >= MIN_GROUP_SIZE) {
                groups += BurstPhotoGroup(
                    representativeId = currentItems.first().id,
                    items = currentItems.toList(),
                    startCaptureTimeMillis = currentStartMillis,
                    endCaptureTimeMillis = previousCaptureMillis,
                    maxAdjacentGapMillis = currentMaxAdjacentGap,
                    maxRepresentativeHammingDistance = currentMaxRepresentativeDistance
                )
            }
            currentItems = mutableListOf()
            currentHashes = mutableListOf()
            currentStartMillis = 0L
            previousCaptureMillis = 0L
            currentMaxAdjacentGap = 0L
            currentMaxRepresentativeDistance = 0
        }

        while (true) {
            coroutineContext.ensureActive()
            val batch = dao.getBurstPhotoCandidates(
                limit = CANDIDATE_BATCH_SIZE,
                maxId = maxMediaAssetId,
                lastDateTakenMillis = lastDateTakenMillis,
                lastId = lastId,
                hasCursor = hasCursor
            )
            if (batch.isEmpty()) break

            for (asset in batch) {
                coroutineContext.ensureActive()
                val captureMillis = asset.dateTakenMillis
                val cached = dao.getMediaAnalysisCache(asset.id)
                    ?.takeIf { it.matchesForPerceptualHash(asset) }
                val hash = cached?.perceptualHash ?: hashCalculator.calculate(asset.contentUri)

                if (hash == null) {
                    unreadableCount++
                    finishCurrentGroup()
                } else {
                    if (cached?.perceptualHash == null) {
                        dao.saveMediaAnalysisCache(
                            MediaAnalysisCache(
                                mediaAssetId = asset.id,
                                sourceSizeBytes = asset.sizeBytes,
                                sourceDateModifiedSeconds = asset.dateModifiedSeconds,
                                perceptualHash = hash,
                                perceptualHashAlgorithmVersion = MediaAnalysisCache.PERCEPTUAL_HASH_ALGORITHM_VERSION,
                                analyzedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }

                    val hashBits = hash.toULong(16)
                    if (currentItems.isEmpty()) {
                        currentItems = mutableListOf(asset)
                        currentHashes = mutableListOf(hashBits)
                        currentStartMillis = captureMillis
                        previousCaptureMillis = captureMillis
                        currentMaxAdjacentGap = 0L
                        currentMaxRepresentativeDistance = 0
                    } else {
                        val gap = (captureMillis - previousCaptureMillis).coerceAtLeast(0L)
                        val span = captureMillis - currentStartMillis
                        val representativeDistance =
                            (currentHashes.first() xor hashBits).countOneBits()
                        val canJoin =
                            gap <= maxAdjacentGapMillis &&
                                span <= maxBurstDurationMillis &&
                                representativeDistance <= maxRepresentativeHammingDistance

                        if (canJoin) {
                            currentItems += asset
                            currentHashes += hashBits
                            previousCaptureMillis = captureMillis
                            currentMaxAdjacentGap = maxOf(currentMaxAdjacentGap, gap)
                            currentMaxRepresentativeDistance =
                                maxOf(currentMaxRepresentativeDistance, representativeDistance)
                        } else {
                            finishCurrentGroup()
                            currentItems = mutableListOf(asset)
                            currentHashes = mutableListOf(hashBits)
                            currentStartMillis = captureMillis
                            previousCaptureMillis = captureMillis
                            currentMaxAdjacentGap = 0L
                            currentMaxRepresentativeDistance = 0
                        }
                    }
                }

                analyzedCount++
                onProgress(analyzedCount, total)
            }
            batch.last().let { last ->
                lastDateTakenMillis = last.dateTakenMillis
                lastId = last.id
                hasCursor = true
            }
        }

        finishCurrentGroup()
        val currentAssets = dao.revalidateAnalysisAssets(groups.flatMap { it.items })
        val validGroups = groups.mapNotNull { group ->
            if (group.representativeId !in currentAssets) return@mapNotNull null
            val currentItems = group.items.mapNotNull { currentAssets[it.id] }
            // Burst membership depends on every adjacent capture-time gap.
            // Removing a stale middle item could incorrectly join two photos
            // that were never adjacent within the configured time window.
            currentItems.takeIf { it.size == group.items.size }?.let { group.copy(items = it) }
        }.sortedWith(
            compareByDescending<BurstPhotoGroup> { it.items.size }
                .thenByDescending { group -> group.items.sumOf { it.sizeBytes } }
        )
        BurstPhotoAnalysisResult(
            groups = validGroups,
            analyzedCount = analyzedCount,
            unreadableCount = unreadableCount
        )
    }

    private fun MediaAnalysisCache.matchesForPerceptualHash(asset: MediaAsset): Boolean =
        sourceSizeBytes == asset.sizeBytes &&
            sourceDateModifiedSeconds == asset.dateModifiedSeconds &&
            perceptualHashAlgorithmVersion == MediaAnalysisCache.PERCEPTUAL_HASH_ALGORITHM_VERSION &&
            perceptualHash?.let { value ->
                value.length == 16 && value.all { char ->
                    char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'
                }
            } == true

    private companion object {
        const val MIN_GROUP_SIZE = 2
        const val MAX_HASH_BITS = 63
        const val DEFAULT_MAX_ADJACENT_GAP_MILLIS = 1_200L
        const val DEFAULT_MAX_BURST_DURATION_MILLIS = 6_000L
        const val DEFAULT_MAX_HAMMING_DISTANCE = 8
        const val MAX_BURST_DURATION_MILLIS = 30_000L
        const val CANDIDATE_BATCH_SIZE = 100
    }
}
