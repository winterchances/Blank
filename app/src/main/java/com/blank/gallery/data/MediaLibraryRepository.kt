package com.blank.gallery.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
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
    fun pagedMedia(mediaType: Int?, query: String) = Pager(
        config = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSource(mediaType, query.trim()) }
    ).flow
    val photoCount = dao.observePhotoCount()
    val videoCount = dao.observeVideoCount()
    val totalBytes = dao.observeTotalBytes()

    /**
     * Hide cached media immediately when the app no longer has permission.
     * Rows and user state remain in Room; a later authorized scan can restore them.
     */
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
        val previousScope = preferences.getString(KEY_LAST_SCAN_SCOPE, null)

        val requiresFullScan =
            permissionScope.contains(":selected") ||
                previousScanAt == 0L ||
                previousScope != permissionScope ||
                previousFullScanAt == 0L ||
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

    suspend fun setSelfOrganizeStatus(id: Long, status: Int) {
        dao.setSelfOrganizeStatus(id, status)
    }

    private companion object {
        const val KEY_LAST_SCAN_AT = "last_scan_at"
        const val KEY_LAST_FULL_SCAN_AT = "last_full_scan_at"
        const val KEY_LAST_SCAN_SCOPE = "last_scan_scope"
        const val NO_ACCESS_SCOPE = "no-access"
        const val SCAN_OVERLAP_SECONDS = 2L
        const val FULL_SCAN_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}
