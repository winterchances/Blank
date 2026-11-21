package com.blank.gallery.data

import android.content.Context

class MediaLibraryRepository(context: Context) {
    private val database = BlankDatabase.getInstance(context)
    private val scanner = MediaStoreScanner(context)

    val media = database.mediaAssetDao().observeAll()
    val photoCount = database.mediaAssetDao().observePhotoCount()
    val videoCount = database.mediaAssetDao().observeVideoCount()
    val totalBytes = database.mediaAssetDao().observeTotalBytes()

    /**
     * Indexes only the media items Android currently exposes under granted permissions.
     * Existing favorite, protection, and review states are preserved on subsequent scans.
     */
    suspend fun scanVisibleMedia(): Int {
        val scanned = scanner.scanVisibleMedia()
        database.mediaAssetDao().syncScanned(scanned)
        return scanned.size
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        database.mediaAssetDao().setFavorite(id, favorite)
    }

    suspend fun setProtected(id: Long, protected: Boolean) {
        database.mediaAssetDao().setProtected(id, protected)
    }

    suspend fun setSelfOrganizeStatus(id: Long, status: Int) {
        database.mediaAssetDao().setSelfOrganizeStatus(id, status)
    }
}
