package com.blank.gallery.domain.analyzer

import android.content.Context
import com.blank.gallery.data.MediaAnalysisCache
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.SimilarPhotoAnalysisResult
import com.blank.gallery.domain.SimilarPhotoGroup
import kotlin.math.max
import kotlin.math.min
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
        val sorted = entries.sortedWith(
            compareByDescending<Pair<MediaAsset, String>> {
                it.first.dateTakenMillis.takeIf { value -> value > 0L }
                    ?: it.first.dateTakenSeconds * 1000L
            }.thenByDescending { it.first.dateAddedSeconds }
                .thenBy { it.first.id }
        )
        val rootHolder = arrayOfNulls<HashTreeNode>(1)
        val groups = mutableListOf<MutableList<MediaAsset>>()

        fun findMatches(
            node: HashTreeNode?,
            target: ULong,
            matches: MutableList<Pair<Int, HashTreeNode>>
        ) {
            if (node == null) return
            val distance = (node.hash xor target).countOneBits()
            if (distance <= threshold) matches += distance to node
            val minimum = max(0, distance - threshold)
            val maximum = distance + threshold
            node.children.forEach { (edgeDistance, child) ->
                if (edgeDistance in minimum..maximum) {
                    findMatches(child, target, matches)
                }
            }
        }

        fun insertRepresentative(node: HashTreeNode?, target: ULong, groupIndex: Int): HashTreeNode {
            if (node == null) return HashTreeNode(target, groupIndex)
            val distance = (node.hash xor target).countOneBits()
            val child = node.children[distance]
            node.children[distance] = insertRepresentative(child, target, groupIndex)
            return node
        }

        for ((asset, hash) in sorted) {
            val bits = hash.toULong(16)
            val matches = mutableListOf<Pair<Int, HashTreeNode>>()
            findMatches(rootHolder[0], bits, matches)
            val nearest = matches.minByOrNull { it.first }?.second
            if (nearest != null) {
                groups[nearest.groupIndex] += asset
            } else {
                val groupIndex = groups.size
                groups += mutableListOf(asset)
                rootHolder[0] = insertRepresentative(rootHolder[0], bits, groupIndex)
            }
        }

        return groups.mapNotNull { members ->
            if (members.size < 2) {
                null
            } else {
                SimilarPhotoGroup(
                    representativeId = members.first().id,
                    items = members
                )
            }
        }.sortedByDescending { group -> group.items.sumOf { it.sizeBytes } }
    }

    private class HashTreeNode(
        val hash: ULong,
        val groupIndex: Int,
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
