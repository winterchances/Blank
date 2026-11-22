package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset

enum class ScreenshotMatchReason {
    FILENAME,
    FOLDER
}

data class ScreenshotPhotoMatch(
    val asset: MediaAsset,
    val reason: ScreenshotMatchReason,
    val matchedText: String
)

data class ScreenshotAnalysisResult(
    val matches: List<ScreenshotPhotoMatch>,
    val analyzedCount: Int
)
