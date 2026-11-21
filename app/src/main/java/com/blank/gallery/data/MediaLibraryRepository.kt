package com.blank.gallery.data

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig

class MediaLibraryRepository(context: Context) {
    private val database = BlankDatabase.getInstance(context)
    private val scanner = MediaStoreScanner(context)

    private val dao = database.mediaAssetDao()

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
}
