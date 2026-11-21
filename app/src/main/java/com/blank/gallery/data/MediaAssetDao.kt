package com.blank.gallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.paging.PagingSource
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE isVisible = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeAll(): Flow<List<MediaAsset>>

    @Query("SELECT * FROM media_assets WHERE isVisible = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun pagingSource(): PagingSource<Int, MediaAsset>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 1")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 2")
    fun observeVideoCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets WHERE isVisible = 1")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT * FROM media_assets WHERE isVisible = 1 AND mediaType = 1 AND selfOrganizeStatus = 0 AND isProtected = 0 ORDER BY dateAddedSeconds DESC, id DESC")
    suspend fun getNextSelfOrganizePhoto(): MediaAsset?

    @Query("SELECT * FROM media_assets WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun findByMediaStoreId(mediaStoreId: Long): MediaAsset?

    @Query("UPDATE media_assets SET isVisible = 0")\n    suspend fun hideAllBeforePermissionScopedScan()\n\n    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(item: MediaAsset)

    @Query("""
        UPDATE media_assets SET
            contentUri = :contentUri,
            displayName = :displayName,
            mimeType = :mimeType,
            mediaType = :mediaType,
            sizeBytes = :sizeBytes,
            dateAddedSeconds = :dateAddedSeconds,
            dateModifiedSeconds = :dateModifiedSeconds,
            width = :width,
            height = :height,
            durationMs = :durationMs
        WHERE mediaStoreId = :mediaStoreId
    """)
    suspend fun updateMediaMetadata(
        mediaStoreId: Long,
        contentUri: String,
        displayName: String,
        mimeType: String?,
        mediaType: Int,
        sizeBytes: Long,
        dateAddedSeconds: Long,
        dateModifiedSeconds: Long,
        width: Int,
        height: Int,
        durationMs: Long
    )

    @Transaction
    suspend fun syncScanned(items: List<MediaAsset>) {
        items.forEach { item ->
            insertIfNew(item)
            updateMediaMetadata(
                mediaStoreId = item.mediaStoreId,
                contentUri = item.contentUri,
                displayName = item.displayName,
                mimeType = item.mimeType,
                mediaType = item.mediaType,
                sizeBytes = item.sizeBytes,
                dateAddedSeconds = item.dateAddedSeconds,
                dateModifiedSeconds = item.dateModifiedSeconds,
                width = item.width,
                height = item.height,
                durationMs = item.durationMs
            )
        }
    }

    @Update
    suspend fun update(item: MediaAsset)

    @Query("UPDATE media_assets SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_assets SET isProtected = :protected WHERE id = :id")
    suspend fun setProtected(id: Long, protected: Boolean)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status WHERE id = :id")
    suspend fun setSelfOrganizeStatus(id: Long, status: Int)
}
