package com.blank.gallery.domain.analyzer

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao

/**
 * Revalidates result members against the latest library state.
 *
 * Analysis may overlap a MediaStore scan, permission-scope change, or a user
 * protection action. Only unchanged sources that are still visible and
 * unprotected are allowed to escape as actionable analysis results.
 */
internal suspend fun MediaAssetDao.revalidateAnalysisAssets(
    analyzedAssets: Collection<MediaAsset>
): Map<Long, MediaAsset> {
    if (analyzedAssets.isEmpty()) return emptyMap()
    val uniqueAssets = analyzedAssets.distinctBy { it.id }
    // Stay below SQLite's bind-parameter limit on devices with older SQLite.
    val currentById = uniqueAssets.chunked(500)
        .flatMap { batch -> getVisibleUnprotectedMediaForIds(batch.map { it.id }) }
        .associateBy { it.id }

    return analyzedAssets.mapNotNull { analyzed ->
        val current = currentById[analyzed.id] ?: return@mapNotNull null
        val sameSource = current.mediaStoreId == analyzed.mediaStoreId &&
            current.contentUri == analyzed.contentUri &&
            current.mediaType == analyzed.mediaType &&
            current.sizeBytes == analyzed.sizeBytes &&
            current.dateModifiedSeconds == analyzed.dateModifiedSeconds &&
            current.displayName == analyzed.displayName &&
            current.relativePath == analyzed.relativePath &&
            current.dateTakenMillis == analyzed.dateTakenMillis
        if (sameSource) analyzed.id to current else null
    }.toMap()
}
