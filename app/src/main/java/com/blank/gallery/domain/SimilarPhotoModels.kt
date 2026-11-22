package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset

/**
 * Visually similar photos. This is a suggestion group, not a byte-identical group.
 * Every member is compared with the representative to avoid transitive chaining.
 */
data class SimilarPhotoGroup(
    val representativeId: Long,
    val items: List<MediaAsset>
) {
    init {
        require(items.size >= 2)
        require(items.any { it.id == representativeId })
    }
}

data class SimilarPhotoAnalysisResult(
    val groups: List<SimilarPhotoGroup>,
    val analyzedCount: Int,
    val unreadableCount: Int
)
