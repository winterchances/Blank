package com.blank.gallery.domain.analyzer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.blank.gallery.data.MediaAnalysisCache
import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.DuplicateAnalysisResult
import com.blank.gallery.domain.DuplicateMediaGroup
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Finds byte-for-byte duplicates using file size only as a cheap pre-filter
 * and SHA-256 as the final equality check.
 *
 * Only visible, unprotected assets are supplied by the DAO. File bytes are
 * read locally in bounded chunks on Dispatchers.IO, and metadata candidates
 * are loaded from Room in small batches.
 */
class DuplicateMediaAnalyzer(
    context: Context,
    private val dao: MediaAssetDao
) {
    private val resolver: ContentResolver =
        context.applicationContext.contentResolver

    suspend fun analyze(
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): DuplicateAnalysisResult = withContext(Dispatchers.IO) {
        val maxMediaAssetId = dao.getMaxMediaAssetId()
        val total = dao.countDuplicateCandidates(maxMediaAssetId)
        val groups = mutableListOf<DuplicateMediaGroup>()
        // DAO ordering keeps each equal-size cohort contiguous, including
        // across page boundaries. Discard singleton hashes as soon as their
        // cohort is complete instead of retaining them for the whole scan.
        var cohortSizeBytes: Long? = null
        var cohortHashGroups = mutableMapOf<String, MutableList<MediaAsset>>()
        var analyzedCount = 0
        var unreadableCount = 0
        var lastSizeBytes = 0L
        var lastId = 0L
        var hasCursor = false

        fun flushCohort() {
            cohortHashGroups.forEach { (hash, assets) ->
                if (assets.size >= 2) {
                    groups += DuplicateMediaGroup(
                        sha256 = hash,
                        items = assets.sortedWith(
                            compareByDescending<MediaAsset> { it.dateTakenSeconds }
                                .thenByDescending { it.dateAddedSeconds }
                        )
                    )
                }
            }
            cohortHashGroups = mutableMapOf()
        }

        while (true) {
            coroutineContext.ensureActive()
            val batch = dao.getDuplicateCandidates(
                limit = CANDIDATE_BATCH_SIZE,
                maxId = maxMediaAssetId,
                lastSizeBytes = lastSizeBytes,
                lastId = lastId,
                hasCursor = hasCursor
            )
            if (batch.isEmpty()) break

            for (asset in batch) {
                coroutineContext.ensureActive()
                if (cohortSizeBytes != null && cohortSizeBytes != asset.sizeBytes) {
                    flushCohort()
                }
                cohortSizeBytes = asset.sizeBytes

                val cached = dao.getMediaAnalysisCache(asset.id)
                    ?.takeIf { it.matches(asset) }

                val hash = cached?.sha256 ?: calculateSha256(asset.contentUri)
                if (hash == null) {
                    unreadableCount++
                } else {
                    if (cached?.sha256 == null) {
                        dao.saveMediaAnalysisCache(
                            MediaAnalysisCache(
                                mediaAssetId = asset.id,
                                sourceSizeBytes = asset.sizeBytes,
                                sourceDateModifiedSeconds = asset.dateModifiedSeconds,
                                sha256 = hash,
                                hashAlgorithmVersion = MediaAnalysisCache.SHA256_ALGORITHM_VERSION,
                                analyzedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    cohortHashGroups.getOrPut(hash) { mutableListOf() }.add(asset)
                }

                analyzedCount++
                onProgress(analyzedCount, total)
            }

            batch.last().let { last ->
                lastSizeBytes = last.sizeBytes
                lastId = last.id
                hasCursor = true
            }
        }

        flushCohort()
        val currentAssets = dao.revalidateAnalysisAssets(groups.flatMap { it.items })
        val validGroups = groups.mapNotNull { group ->
            val currentItems = group.items.mapNotNull { currentAssets[it.id] }
            currentItems.takeIf { it.size >= 2 }?.let { group.copy(items = it) }
        }
        val sortedGroups = validGroups.sortedByDescending { group ->
            group.items.sumOf { it.sizeBytes }
        }

        DuplicateAnalysisResult(
            groups = sortedGroups,
            analyzedCount = analyzedCount,
            unreadableCount = unreadableCount
        )
    }

    private suspend fun calculateSha256(contentUri: String): String? {
        coroutineContext.ensureActive()
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = resolver.openInputStream(Uri.parse(contentUri)) ?: return null
            input.use { stream ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (count > 0) digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
        } catch (error: IOException) {
            null
        } catch (error: SecurityException) {
            null
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    private fun MediaAnalysisCache.matches(asset: MediaAsset): Boolean =
        sourceSizeBytes == asset.sizeBytes &&
            sourceDateModifiedSeconds == asset.dateModifiedSeconds &&
            hashAlgorithmVersion == MediaAnalysisCache.SHA256_ALGORITHM_VERSION &&
            !sha256.isNullOrBlank()

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val CANDIDATE_BATCH_SIZE = 200
    }
}
