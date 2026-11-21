package com.blank.gallery.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MediaFolder(
    val relativePath: String,
    val itemCount: Int
)

data class MediaMonth(
    val monthKey: String,
    val itemCount: Int
)

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE isVisible = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeAll(): Flow<List<MediaAsset>>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1
          AND (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:query = '' OR displayName LIKE '%' || :query || '%')
          AND (:folderPath IS NULL OR relativePath = :folderPath)
          AND (:monthKey IS NULL OR strftime('%Y-%m', datetime(
              CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END,
              'unixepoch', 'localtime'
          )) = :monthKey)
        ORDER BY CASE WHEN :sortBySize = 1 THEN sizeBytes END DESC,
                 CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
    """)
    fun pagingSource(mediaType: Int?, query: String, folderPath: String?, monthKey: String?, sortBySize: Int): PagingSource<Int, MediaAsset>

    @Query("""
        SELECT relativePath, COUNT(*) AS itemCount
        FROM media_assets
        WHERE isVisible = 1 AND relativePath != ''
        GROUP BY relativePath
        ORDER BY relativePath COLLATE NOCASE ASC
    """)
    fun observeFolders(): Flow<List<MediaFolder>>

    @Query("""
        SELECT strftime('%Y-%m', datetime(
            CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END,
            'unixepoch', 'localtime'
        )) AS monthKey, COUNT(*) AS itemCount
        FROM media_assets
        WHERE isVisible = 1
        GROUP BY monthKey
        ORDER BY monthKey DESC
    """)
    fun observeMonths(): Flow<List<MediaMonth>>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 1")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 2")
    fun observeVideoCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets WHERE isVisible = 1")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT * FROM media_assets WHERE isVisible = 1 AND mediaType = 1 AND selfOrganizeStatus = 0 AND isProtected = 0 ORDER BY CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC, id DESC")
    suspend fun getNextSelfOrganizePhoto(): MediaAsset?

    @Query("SELECT * FROM media_assets WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun findByMediaStoreId(mediaStoreId: Long): MediaAsset?

    @Query("UPDATE media_assets SET isVisible = 0")
    suspend fun hideAllBeforePermissionScopedScan()

    @Query("UPDATE media_assets SET isVisible = CASE WHEN lastSeenScanToken = :scanToken THEN 1 ELSE 0 END")
    suspend fun completePermissionScopedScan(scanToken: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(item: MediaAsset)

    @Query("""
        UPDATE media_assets SET
            contentUri = :contentUri,
            displayName = :displayName,
            relativePath = :relativePath,
            mimeType = :mimeType,
            mediaType = :mediaType,
            sizeBytes = :sizeBytes,
            dateAddedSeconds = :dateAddedSeconds,
            dateTakenSeconds = :dateTakenSeconds,
            dateModifiedSeconds = :dateModifiedSeconds,
            width = :width,
            height = :height,
            durationMs = :durationMs,
            lastSeenScanToken = :scanToken,
            isVisible = 1
        WHERE mediaStoreId = :mediaStoreId
    """)
    suspend fun updateMediaMetadata(
        mediaStoreId: Long,
        contentUri: String,
        displayName: String,
        relativePath: String,
        mimeType: String?,
        mediaType: Int,
        sizeBytes: Long,
        dateAddedSeconds: Long,
        dateTakenSeconds: Long,
        dateModifiedSeconds: Long,
        width: Int,
        height: Int,
        durationMs: Long,
        scanToken: String
    )

    @Transaction
    suspend fun syncScanned(items: List<MediaAsset>, scanToken: String) {
        items.forEach { item ->
            insertIfNew(item.copy(lastSeenScanToken = scanToken))
            updateMediaMetadata(
                mediaStoreId = item.mediaStoreId,
                contentUri = item.contentUri,
                displayName = item.displayName,
                relativePath = item.relativePath,
                mimeType = item.mimeType,
                mediaType = item.mediaType,
                sizeBytes = item.sizeBytes,
                dateAddedSeconds = item.dateAddedSeconds,
                dateTakenSeconds = item.dateTakenSeconds,
                dateModifiedSeconds = item.dateModifiedSeconds,
                width = item.width,
                height = item.height,
                durationMs = item.durationMs,
                scanToken = scanToken
            )
        }
    }

    @Update
    suspend fun update(item: MediaAsset)

    @Query("UPDATE media_assets SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_assets SET isProtected = :protected WHERE id = :id")
    suspend fun setProtected(id: Long, protected: Boolean)

    @Query("UPDATE media_assets SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun setFavoriteForIds(ids: List<Long>, favorite: Boolean)

    @Query("UPDATE media_assets SET isProtected = :protected WHERE id IN (:ids)")
    suspend fun setProtectedForIds(ids: List<Long>, protected: Boolean)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status WHERE id = :id")
    suspend fun setSelfOrganizeStatus(id: Long, status: Int)
}
