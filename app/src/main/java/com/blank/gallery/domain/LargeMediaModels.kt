package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset

data class LargeMediaAnalysisResult(
    val minimumSizeBytes: Long,
    val items: List<MediaAsset>,
    val analyzedCount: Int,
    val totalSizeBytes: Long,
    val photoCount: Int,
    val videoCount: Int
)
