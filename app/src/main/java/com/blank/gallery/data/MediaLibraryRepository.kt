package com.blank.gallery.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import androidx.paging.insertSeparators
import com.blank.gallery.ui.screens.AlbumGridItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MediaLibraryRepository(context: Context) {
    private val database = BlankDatabase.getInstance(context)
    private val scanner = MediaStoreScanner(context)
    private val dao = database.mediaAssetDao()
    private val preferences = context.applicationContext.getSharedPreferences(
        "blank_local_state",
        Context.MODE_PRIVATE
    )
    private val _lastScanAt = MutableStateFlow(
        preferences.getLong(KEY_LAST_SCAN_AT, 0L).takeIf { it > 0L }
    )

    val lastScanAt = _lastScanAt.asStateFlow()
    fun pagedMedia(mediaType: Int?, query: String, folderPath: String?, monthKey: String?, sortBySize: Boolean) = Pager(
        config = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            dao.pagingSource(mediaType, query.trim(), folderPath, monthKey, if (sortBySize) 1 else 0)
        }
    ).flow.map { pagingData ->
        val mediaItems = pagingData.map<MediaAsset, AlbumGridItem> { AlbumGridItem.Media(it) }
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
        val now = System.currentTimeMillis()
        val previousScanAt = preferences.getLong(KEY_LAST_SCAN_AT, 0L)
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
            (previousScanAt / 1000L - SCAN_OVERLAP_SECONDS).coerceAtLeast(0L)
        }

        val scanToken = UUID.randomUUID().toString()
        var scannedCount = 0
        scanner.scanVisibleMedia(
            includeImages = includeImages,
            includeVideos = includeVideos,
            modifiedSinceSeconds = modifiedSinceSeconds
        ) { batch ->
            dao.syncScanned(batch, scanToken)
            scannedCount += batch.size
            onProgress(scannedCount)
        }

        if (requiresFullScan) {
            dao.completePermissionScopedScan(scanToken)
        }

        val completedAt = System.currentTimeMillis()
        val editor = preferences.edit()
            .putLong(KEY_LAST_SCAN_AT, completedAt)
            .putString(KEY_LAST_SCAN_SCOPE, permissionScope)
        if (requiresFullScan) {
            editor.putLong(KEY_LAST_FULL_SCAN_AT, completedAt)
                .putInt(KEY_MEDIA_INDEX_VERSION, MEDIA_INDEX_VERSION)
        }
        editor.apply()
        _lastScanAt.value = completedAt
        return scannedCount
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
        if (ids.isNotEmpty()) dao.hideFromLibraryForIds(ids)
    }

    suspend fun nextSelfOrganizePhoto(random: Boolean): MediaAsset? =
        if (random) dao.getNextRandomSelfOrganizePhoto() else dao.getNextOrderedSelfOrganizePhoto()

    suspend fun applySelfOrganizeDecision(id: Long, status: Int) {
        dao.applySelfOrganizeDecision(id, status)
    }

    suspend fun undoLastSelfOrganizeDecision(): Boolean =
        dao.undoLastSelfOrganizeDecision()

    suspend fun setSelfOrganizeStatus(id: Long, status: Int) {
        dao.setSelfOrganizeStatus(id, status)
    }

    private companion object {
        const val KEY_LAST_SCAN_AT = "last_scan_at"
        const val KEY_LAST_FULL_SCAN_AT = "last_full_scan_at"
        const val KEY_LAST_SCAN_SCOPE = "last_scan_scope"
        const val KEY_MEDIA_INDEX_VERSION = "media_index_version"
        const val MEDIA_INDEX_VERSION = 1
        const val NO_ACCESS_SCOPE = "no-access"
        const val SCAN_OVERLAP_SECONDS = 2L
        const val FULL_SCAN_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}
