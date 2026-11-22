package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset

/**
 * A confirmed byte-identical group. Similar-looking media must use a
 * different result type and must never be merged into this result.
 */
data class DuplicateMediaGroup(
    val sha256: String,
    val items: List<MediaAsset>
) {
    init {
        require(items.size >= 2)
    }
}

data class DuplicateAnalysisResult(
    val groups: List<DuplicateMediaGroup>,
    val analyzedCount: Int,
    val unreadableCount: Int
)
