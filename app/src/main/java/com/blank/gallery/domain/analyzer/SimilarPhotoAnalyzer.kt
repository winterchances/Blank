package com.blank.gallery.domain.analyzer

import android.content.Context
import com.blank.gallery.data.MediaAnalysisCache
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.SimilarPhotoAnalysisResult
import com.blank.gallery.domain.SimilarPhotoGroup
import kotlin.math.max
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private val hashCalculator = PerceptualHashCalculator(context)

    suspend fun analyze(
        maxHammingDistance: Int = DEFAULT_MAX_HAMMING_DISTANCE,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): SimilarPhotoAnalysisResult = withContext(Dispatchers.IO) {
        require(maxHammingDistance in 0..MAX_HASH_BITS)
        val maxMediaAssetId = dao.getMaxMediaAssetId()
        val total = dao.countSimilarPhotoCandidates(maxMediaAssetId)
        val groupMembers = mutableMapOf<Int, MutableList<MediaAsset>>()
        val rootHolder = arrayOfNulls<HashTreeNode>(1)
        var nextGroupIndex = 0
        var analyzedCount = 0
        var unreadableCount = 0
        var lastCaptureTimeMillis = 0L
        var lastDateAddedSeconds = 0L
        var lastId = 0L
        var hasCursor = false

        fun findMatches(
            node: HashTreeNode?,
            target: ULong,
            matches: MutableList<Pair<Int, HashTreeNode>>
        ) {
            if (node == null) return
            val distance = (node.hash xor target).countOneBits()
            if (distance <= maxHammingDistance) matches += distance to node
            val minimum = max(0, distance - maxHammingDistance)
            val maximum = distance + maxHammingDistance
            node.children.forEach { (edgeDistance, child) ->
                if (edgeDistance in minimum..maximum) {
                    findMatches(child, target, matches)
                }
            }
        }

        fun insertRepresentative(
            node: HashTreeNode?,
            target: ULong,
            representativeId: Long,
            groupIndex: Int
        ): HashTreeNode {
            if (node == null) return HashTreeNode(target, groupIndex, representativeId)
            val distance = (node.hash xor target).countOneBits()
            val child = node.children[distance]
            node.children[distance] = insertRepresentative(child, target, representativeId, groupIndex)
            return node
        }

        suspend fun addToRepresentativeGroups(asset: MediaAsset, hash: String) {
            val bits = hash.toULong(16)
            val matches = mutableListOf<Pair<Int, HashTreeNode>>()
            findMatches(rootHolder[0], bits, matches)
            val nearest = matches.minByOrNull { it.first }?.second
            if (nearest != null) {
                val existingMembers = groupMembers[nearest.groupIndex]
                if (existingMembers == null) {
                    val representative = dao.findById(nearest.representativeId) ?: return
                    groupMembers[nearest.groupIndex] = mutableListOf(representative, asset)
                } else {
                    existingMembers += asset
                }
            } else {
                val groupIndex = nextGroupIndex++
                // Keep only representative IDs in the hash tree. Full media
                // records are fetched only when a second member confirms a group.
                rootHolder[0] = insertRepresentative(
                    rootHolder[0],
                    bits,
                    asset.id,
                    groupIndex
                )
            }
        }

        while (true) {
            coroutineContext.ensureActive()
            val batch = dao.getSimilarPhotoCandidates(
                limit = CANDIDATE_BATCH_SIZE,
                maxId = maxMediaAssetId,
                lastCaptureTimeMillis = lastCaptureTimeMillis,
                lastDateAddedSeconds = lastDateAddedSeconds,
                lastId = lastId,
                hasCursor = hasCursor
            )
            if (batch.isEmpty()) break

            for (asset in batch) {
                coroutineContext.ensureActive()
                val cached = dao.getMediaAnalysisCache(asset.id)
                    ?.takeIf { it.matchesForPerceptualHash(asset) }
                val hash = cached?.perceptualHash ?: hashCalculator.calculate(asset.contentUri)
                if (hash == null) {
                    unreadableCount++
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
                    addToRepresentativeGroups(asset, hash)
                }
                analyzedCount++
                onProgress(analyzedCount, total)
            }
            batch.last().let { last ->
                lastCaptureTimeMillis = if (last.dateTakenMillis > 0L) {
                    last.dateTakenMillis
                } else {
                    last.dateTakenSeconds * 1000L
                }
                lastDateAddedSeconds = last.dateAddedSeconds
                lastId = last.id
                hasCursor = true
            }
        }

        val groups = groupMembers.toSortedMap().values.map { members ->
            SimilarPhotoGroup(
                representativeId = members.first().id,
                items = members
            )
        }
        val currentAssets = dao.revalidateAnalysisAssets(groups.flatMap { it.items })
        val validGroups = groups.mapNotNull { group ->
            if (group.representativeId !in currentAssets) return@mapNotNull null
            val currentItems = group.items.mapNotNull { currentAssets[it.id] }
            currentItems.takeIf { it.size >= 2 }?.let { group.copy(items = it) }
        }.sortedByDescending { group -> group.items.sumOf { it.sizeBytes } }

        SimilarPhotoAnalysisResult(validGroups, analyzedCount, unreadableCount)
    }

    private class HashTreeNode(
        val hash: ULong,
        val groupIndex: Int,
        val representativeId: Long,
        val children: MutableMap<Int, HashTreeNode> = mutableMapOf()
    )

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
        const val MAX_HASH_BITS = 63
        const val DEFAULT_MAX_HAMMING_DISTANCE = 6
        const val CANDIDATE_BATCH_SIZE = 100
    }
}
