package com.blank.gallery.domain.analyzer

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaAssetDao
import com.blank.gallery.domain.ScreenshotAnalysisResult
import com.blank.gallery.domain.ScreenshotMatchReason
import com.blank.gallery.domain.ScreenshotPhotoMatch
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Conservative screenshot identification using explicit filename/folder signals.
 * Aspect ratio alone is deliberately not used because posters and wallpapers
 * commonly share screenshot-like dimensions.
 */
class ScreenshotAnalyzer(
    private val dao: MediaAssetDao
) {
    suspend fun analyze(
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): ScreenshotAnalysisResult = withContext(Dispatchers.IO) {
        val total = dao.countScreenshotCandidates()
        val matches = mutableListOf<ScreenshotPhotoMatch>()
        var analyzedCount = 0
        var offset = 0

        while (offset < total) {
            coroutineContext.ensureActive()
            val batch = dao.getScreenshotCandidates(BATCH_SIZE, offset)
            if (batch.isEmpty()) break

            for (asset in batch) {
                coroutineContext.ensureActive()
                identify(asset)?.let(matches::add)
                analyzedCount++
                onProgress(analyzedCount, total)
            }
            offset += batch.size
        }

        val currentAssets = dao.revalidateAnalysisAssets(matches.map { it.asset })
        val currentMatches = matches.mapNotNull { match ->
            currentAssets[match.asset.id]?.let { match.copy(asset = it) }
        }
        ScreenshotAnalysisResult(
            matches = currentMatches,
            analyzedCount = analyzedCount
        )
    }

    private fun identify(asset: MediaAsset): ScreenshotPhotoMatch? {
        val fileName = asset.displayName.substringAfterLast('/').trim()
        val normalizedName = fileName.lowercase(Locale.ROOT)
        val filenamePrefix = FILENAME_PREFIXES.firstOrNull(normalizedName::startsWith)
        if (filenamePrefix != null) {
            return ScreenshotPhotoMatch(
                asset = asset,
                reason = ScreenshotMatchReason.FILENAME,
                matchedText = fileName
            )
        }

        val pathSegments = asset.relativePath
            .replace('\\', '/')
            .split('/')
            .filter(String::isNotBlank)
        val matchedFolder = pathSegments.firstOrNull { segment ->
            segment.lowercase(Locale.ROOT) in SCREENSHOT_FOLDERS
        } ?: return null

        return ScreenshotPhotoMatch(
            asset = asset,
            reason = ScreenshotMatchReason.FOLDER,
            matchedText = matchedFolder
        )
    }

    private companion object {
        const val BATCH_SIZE = 200

        val FILENAME_PREFIXES = listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "screen_capture",
            "screen-capture",
            "screen capture",
            "screencapture",
            "截屏",
            "截图",
            "螢幕截圖",
            "屏幕截图"
        )

        val SCREENSHOT_FOLDERS = setOf(
            "screenshots",
            "screenshot",
            "screen_shots",
            "screen-shots",
            "screen shots",
            "screen captures",
            "screen_captures",
            "screen-captures",
            "截屏",
            "截图",
            "螢幕截圖",
            "屏幕截图"
        )
    }
}
