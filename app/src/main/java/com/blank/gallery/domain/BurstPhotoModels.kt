package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset

/**
 * A conservative burst candidate group. Membership requires both a short
 * capture-time gap and visual similarity to the group's representative.
 */
data class BurstPhotoGroup(
    val representativeId: Long,
    val items: List<MediaAsset>,
    val startCaptureTimeMillis: Long,
    val endCaptureTimeMillis: Long,
    val maxAdjacentGapMillis: Long,
    val maxRepresentativeHammingDistance: Int
) {
    init {
        require(items.size >= 2)
        require(items.any { it.id == representativeId })
        require(endCaptureTimeMillis >= startCaptureTimeMillis)
    }
}

data class BurstPhotoAnalysisResult(
    val groups: List<BurstPhotoGroup>,
    val analyzedCount: Int,
    val unreadableCount: Int
)
