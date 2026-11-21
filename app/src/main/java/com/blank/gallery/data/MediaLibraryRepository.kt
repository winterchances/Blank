package com.blank.gallery.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val media = dao.observeAll()
    val pagedMedia = Pager(
        config = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSource() }
    ).flow
    val photoCount = dao.observePhotoCount()
    val videoCount = dao.observeVideoCount()
    val totalBytes = dao.observeTotalBytes()

    /**
     * Indexes only the media items Android currently exposes under granted permissions.
     * Existing favorite, protection, and review states are preserved on subsequent scans.
     */
    suspend fun hideAllFromCurrentLibrary() {
        dao.hideAllBeforePermissionScopedScan()
    }

    suspend fun scanVisibleMedia(
        includeImages: Boolean,
        includeVideos: Boolean,
        onProgress: suspend (Int) -> Unit = {}
    ): Int {
        dao.hideAllBeforePermissionScopedScan()
        var scannedCount = 0
        scanner.scanVisibleMedia(includeImages, includeVideos) { batch ->
            dao.syncScanned(batch)
            scannedCount += batch.size
            onProgress(scannedCount)
        }
        val completedAt = System.currentTimeMillis()
        preferences.edit().putLong(KEY_LAST_SCAN_AT, completedAt).apply()
        _lastScanAt.value = completedAt
        return scannedCount
    }

    private companion object {
        const val KEY_LAST_SCAN_AT = "last_scan_at"
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
}
