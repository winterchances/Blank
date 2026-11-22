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
 * read locally in bounded chunks on Dispatchers.IO.
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
        val candidates = dao.getDuplicateCandidates()
        val groups = mutableListOf<DuplicateMediaGroup>()
        var analyzedCount = 0
        var unreadableCount = 0

        val hashGroups = mutableMapOf<String, MutableList<MediaAsset>>()
        candidates.forEachIndexed { index, asset ->
            coroutineContext.ensureActive()
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
                hashGroups.getOrPut(hash) { mutableListOf() }.add(asset)
            }
            analyzedCount++
            onProgress(index + 1, candidates.size)
        }

        hashGroups.forEach { (hash, assets) ->
            if (assets.size > 1) {
                groups += DuplicateMediaGroup(
                    sha256 = hash,
                    items = assets.sortedWith(
                        compareByDescending<MediaAsset> { it.dateTakenSeconds }
                            .thenByDescending { it.dateAddedSeconds }
                    )
                )
            }
        }

        DuplicateAnalysisResult(
            groups = groups.sortedByDescending { group ->
                group.items.sumOf { it.sizeBytes }
            },
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
        }
    }

    private fun MediaAnalysisCache.matches(asset: MediaAsset): Boolean =
        sourceSizeBytes == asset.sizeBytes &&
            sourceDateModifiedSeconds == asset.dateModifiedSeconds &&
            hashAlgorithmVersion == MediaAnalysisCache.SHA256_ALGORITHM_VERSION &&
            !sha256.isNullOrBlank()

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
