package com.blank.gallery.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingConfig
import androidx.paging.map
import androidx.paging.insertSeparators
import com.blank.gallery.ui.screens.AlbumGridItem
import com.blank.gallery.domain.DuplicateAnalysisResult
import com.blank.gallery.domain.SimilarPhotoAnalysisResult
import com.blank.gallery.domain.BurstPhotoAnalysisResult
import com.blank.gallery.domain.ScreenshotAnalysisResult
import com.blank.gallery.domain.LargeMediaAnalysisResult
import com.blank.gallery.domain.SmartAnalysisTaskCoordinator
import com.blank.gallery.domain.SmartAnalysisTaskState
import com.blank.gallery.domain.SmartAnalysisType
import com.blank.gallery.domain.analyzer.DuplicateMediaAnalyzer
import com.blank.gallery.domain.analyzer.SimilarPhotoAnalyzer
import com.blank.gallery.domain.analyzer.BurstPhotoAnalyzer
import com.blank.gallery.domain.analyzer.ScreenshotAnalyzer
import com.blank.gallery.domain.analyzer.LargeMediaAnalyzer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map as flowMap
import java.util.UUID

class MediaLibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = BlankDatabase.getInstance(appContext)
    private val scanner = MediaStoreScanner(context)
    private val dao = database.mediaAssetDao()
    private val duplicateMediaAnalyzer = DuplicateMediaAnalyzer(context, dao)
    private val similarPhotoAnalyzer = SimilarPhotoAnalyzer(context, dao)
    private val burstPhotoAnalyzer = BurstPhotoAnalyzer(context, dao)
    private val screenshotAnalyzer = ScreenshotAnalyzer(dao)
    private val largeMediaAnalyzer = LargeMediaAnalyzer(dao)
    private val smartAnalysisCoordinator = SmartAnalysisTaskCoordinator()
    private val preferences = context.applicationContext.getSharedPreferences(
        "blank_local_state",
        Context.MODE_PRIVATE
    )
    private val _lastScanAt = MutableStateFlow(
        preferences.getLong(KEY_LAST_SCAN_AT, 0L).takeIf { it > 0L }
    )

    val lastScanAt = _lastScanAt.asStateFlow()
    val smartAnalysisStates = smartAnalysisCoordinator.states

    fun pagedMedia(mediaType: Int?, query: String, folderPath: String?, monthKey: String?, sortBySize: Boolean): Flow<PagingData<AlbumGridItem>> = Pager<Int, MediaAsset>(
        config = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            dao.pagingSource(mediaType, query.trim(), folderPath, monthKey, if (sortBySize) 1 else 0)
        }
    ).flow.flowMap { pagingData: PagingData<MediaAsset> ->
        val mediaItems: PagingData<AlbumGridItem> = pagingData.map { AlbumGridItem.Media(it) }
        if (sortBySize) {
            mediaItems
        } else {
            mediaItems.insertSeparators { before, after ->
                val current = after as? AlbumGridItem.Media ?: return@insertSeparators null
                val previous = before as? AlbumGridItem.Media
                val currentDate = current.asset.albumLocalDate()
                val previousDate = previous?.asset?.albumLocalDate()
                if (currentDate != previousDate) {
                    AlbumGridItem.DateHeader(currentDate, currentDate.toAlbumDateLabel())
                } else null
            }
        }
    }
    val folders = dao.observeFolders()
    val months = dao.observeMonths()
    val photoCount = dao.observePhotoCount()
    val videoCount = dao.observeVideoCount()
    val totalBytes = dao.observeTotalBytes()
    val selfOrganizeSummary = dao.observeSelfOrganizeSummary()
    val canUndoSelfOrganize = dao.observeCanUndoSelfOrganize()
    val selfOrganizePendingDelete = dao.observeSelfOrganizePendingDelete()

    /**
     * Hide cached media immediately when the app no longer has permission.
     * Rows and user state remain in Room; a later authorized scan can restore them.
     */
    private fun MediaAsset.albumLocalDate(): LocalDate {
        val seconds = dateTakenSeconds.takeIf { it > 0L } ?: dateAddedSeconds
        return Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun LocalDate.toAlbumDateLabel(): String {
        val today = LocalDate.now()
        return when (this) {
            today -> "今天"
            today.minusDays(1) -> "昨天"
            else -> format(DateTimeFormatter.ofPattern("yyyy年M月d日", java.util.Locale.getDefault()))
        }
    }

    suspend fun hideAllFromCurrentLibrary() {
        dao.hideAllBeforePermissionScopedScan()
        preferences.edit().putString(KEY_LAST_SCAN_SCOPE, NO_ACCESS_SCOPE).apply()
    }

    /**
     * First scan, permission-scope changes, and daily reconciliation use a full scan.
     * Otherwise only newly added or modified MediaStore rows are queried.
     * Full-scan visibility is finalized with a scan token, so interrupted scans do not
     * incorrectly hide rows that have not yet been reached.
     */
    suspend fun scanVisibleMedia(
        includeImages: Boolean,
        includeVideos: Boolean,
        permissionScope: String,
        onProgress: suspend (Int) -> Unit = {}
    ): Int {
        if (MediaPermission.accessScope(appContext) != permissionScope) {
            dao.hideAllBeforePermissionScopedScan()
            throw SecurityException("Media access scope changed before scan")
        }
        val now = System.currentTimeMillis()
        val scanStartedAtSeconds = now / 1000L
        val previousScanAt = preferences.getLong(KEY_LAST_SCAN_AT, 0L)
        val previousScanCutoffSeconds = preferences.getLong(
            KEY_LAST_SCAN_CUTOFF_SECONDS,
            previousScanAt / 1000L
        )
        val previousFullScanAt = preferences.getLong(KEY_LAST_FULL_SCAN_AT, 0L)
        val captureTimeIndexVersion = preferences.getInt(KEY_MEDIA_INDEX_VERSION, 0)
        val previousScope = preferences.getString(KEY_LAST_SCAN_SCOPE, null)

        val requiresFullScan =
            permissionScope.contains(":selected") ||
                previousScanAt == 0L ||
                previousScope != permissionScope ||
                previousFullScanAt == 0L ||
                captureTimeIndexVersion < MEDIA_INDEX_VERSION ||
                now - previousFullScanAt >= FULL_SCAN_INTERVAL_MS

        val permissionScopeChanged = previousScope != permissionScope
        if (permissionScopeChanged || permissionScope.contains(":selected")) {
            dao.hideAllBeforePermissionScopedScan()
        }

        val modifiedSinceSeconds = if (requiresFullScan) {
            null
        } else {
            (previousScanCutoffSeconds - SCAN_OVERLAP_SECONDS).coerceAtLeast(0L)
        }

        val scanToken = UUID.randomUUID().toString()
        var scannedCount = 0
        try {
            scanner.scanVisibleMedia(
                includeImages = includeImages,
                includeVideos = includeVideos,
                modifiedSinceSeconds = modifiedSinceSeconds
            ) { batch ->
                dao.syncScanned(batch, scanToken)
                scannedCount += batch.size
                onProgress(scannedCount)
            }
        } catch (error: SecurityException) {
            // Some batches may already have been synchronized under the old
            // permission scope. Hide them until a complete authorized scan.
            dao.hideAllBeforePermissionScopedScan()
            throw error
        } catch (error: Exception) {
            // A provider may surface a permission transition as a non-security
            // failure. Do not leave partially indexed rows visible if access changed.
            if (MediaPermission.accessScope(appContext) != permissionScope) {
                dao.hideAllBeforePermissionScopedScan()
            }
            throw error
        }

        if (MediaPermission.accessScope(appContext) != permissionScope) {
            dao.hideAllBeforePermissionScopedScan()
            throw SecurityException("Media access scope changed during scan")
        }

        if (requiresFullScan) {
            dao.completePermissionScopedScan(scanToken)
        }

        // Recheck after visibility finalization as well; permission can change
        // between the first check and the Room transaction above.
        if (MediaPermission.accessScope(appContext) != permissionScope) {
            dao.hideAllBeforePermissionScopedScan()
            throw SecurityException("Media access scope changed while finalizing scan")
        }

        val completedAt = System.currentTimeMillis()
        val editor = preferences.edit()
            .putLong(KEY_LAST_SCAN_AT, completedAt)
            // Use the scan start as the next incremental lower bound. Changes
            // made while this scan is running are then picked up next time,
            // even if their sort position was already passed by the cursor.
            .putLong(KEY_LAST_SCAN_CUTOFF_SECONDS, scanStartedAtSeconds)
            .putString(KEY_LAST_SCAN_SCOPE, permissionScope)
        if (requiresFullScan) {
            editor.putLong(KEY_LAST_FULL_SCAN_AT, completedAt)
                .putInt(KEY_MEDIA_INDEX_VERSION, MEDIA_INDEX_VERSION)
        }
        editor.apply()
        _lastScanAt.value = completedAt
        return scannedCount
    }

    suspend fun analyzeDuplicateMedia(
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): DuplicateAnalysisResult = smartAnalysisCoordinator.execute(
        type = SmartAnalysisType.DUPLICATES,
        parameterKey = "sha256-v1"
    ) { reportProgress ->
        duplicateMediaAnalyzer.analyze { completed, total ->
            reportProgress(completed, total)
            onProgress(completed, total)
        }
    }

    suspend fun analyzeSimilarPhotos(
        maxHammingDistance: Int = 6,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): SimilarPhotoAnalysisResult = smartAnalysisCoordinator.execute(
        type = SmartAnalysisType.SIMILAR_PHOTOS,
        parameterKey = "maxHammingDistance=$maxHammingDistance"
    ) { reportProgress ->
        similarPhotoAnalyzer.analyze(maxHammingDistance) { completed, total ->
            reportProgress(completed, total)
            onProgress(completed, total)
        }
    }

    suspend fun analyzeBurstPhotos(
        maxAdjacentGapMillis: Long = 1_200L,
        maxBurstDurationMillis: Long = 6_000L,
        maxRepresentativeHammingDistance: Int = 8,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BurstPhotoAnalysisResult = smartAnalysisCoordinator.execute(
        type = SmartAnalysisType.BURST_PHOTOS,
        parameterKey = "gap=$maxAdjacentGapMillis;duration=$maxBurstDurationMillis;distance=$maxRepresentativeHammingDistance"
    ) { reportProgress ->
        burstPhotoAnalyzer.analyze(
            maxAdjacentGapMillis = maxAdjacentGapMillis,
            maxBurstDurationMillis = maxBurstDurationMillis,
            maxRepresentativeHammingDistance = maxRepresentativeHammingDistance
        ) { completed, total ->
            reportProgress(completed, total)
            onProgress(completed, total)
        }
    }

    suspend fun analyzeScreenshots(
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): ScreenshotAnalysisResult = smartAnalysisCoordinator.execute(
        type = SmartAnalysisType.SCREENSHOTS,
        parameterKey = "filename-folder-rules-v1"
    ) { reportProgress ->
        screenshotAnalyzer.analyze { completed, total ->
            reportProgress(completed, total)
            onProgress(completed, total)
        }
    }

    suspend fun analyzeLargeMedia(
        minimumSizeBytes: Long = 100L * 1024L * 1024L,
        onProgress: suspend (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): LargeMediaAnalysisResult = smartAnalysisCoordinator.execute(
        type = SmartAnalysisType.LARGE_MEDIA,
        parameterKey = "minimumSizeBytes=$minimumSizeBytes"
    ) { reportProgress ->
        largeMediaAnalyzer.analyze(minimumSizeBytes) { completed, total ->
            reportProgress(completed, total)
            onProgress(completed, total)
        }
    }

    suspend fun cancelSmartAnalysis(type: SmartAnalysisType) {
        smartAnalysisCoordinator.cancel(type)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.setFavorite(id, favorite)
    }

    suspend fun setProtected(id: Long, protected: Boolean) {
        dao.setProtected(id, protected)
    }

    suspend fun setFavoriteForIds(ids: List<Long>, favorite: Boolean) {
        if (ids.isNotEmpty()) dao.setFavoriteForIds(ids, favorite)
    }

    suspend fun setProtectedForIds(ids: List<Long>, protected: Boolean) {
        if (ids.isNotEmpty()) dao.setProtectedForIds(ids, protected)
    }

    /** Retain local records while removing trashed items from the active grid. */
    suspend fun hideFromLibraryForIds(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.finalizeTrashedMedia(ids)
    }

    suspend fun nextSelfOrganizePhoto(random: Boolean): MediaAsset? =
        if (random) dao.getNextRandomSelfOrganizePhoto() else dao.getNextOrderedSelfOrganizePhoto()

    suspend fun applySelfOrganizeDecision(id: Long, status: Int) {
        dao.applySelfOrganizeDecision(id, status)
    }

    suspend fun undoLastSelfOrganizeDecision(): MediaAsset? =
        dao.undoLastSelfOrganizeDecision()

    suspend fun setSelfOrganizeStatus(id: Long, status: Int) {
        dao.setSelfOrganizeStatus(id, status)
    }

    suspend fun cancelSelfOrganizePendingDelete(id: Long) {
        dao.cancelSelfOrganizePendingDelete(id)
    }

    suspend fun currentPendingSelfOrganizePhotos(ids: List<Long>): List<MediaAsset> =
        if (ids.isEmpty()) emptyList() else dao.getVisibleUnprotectedPendingSelfOrganizePhotos(ids)

    private companion object {
        const val KEY_LAST_SCAN_AT = "last_scan_at"
        const val KEY_LAST_SCAN_CUTOFF_SECONDS = "last_scan_cutoff_seconds"
        const val KEY_LAST_FULL_SCAN_AT = "last_full_scan_at"
        const val KEY_LAST_SCAN_SCOPE = "last_scan_scope"
        const val KEY_MEDIA_INDEX_VERSION = "media_index_version"
        const val MEDIA_INDEX_VERSION = 2
        const val NO_ACCESS_SCOPE = "no-access"
        const val SCAN_OVERLAP_SECONDS = 2L
        const val FULL_SCAN_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}