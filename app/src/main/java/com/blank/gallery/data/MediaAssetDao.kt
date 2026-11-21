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

data class SelfOrganizeSummary(
    val totalCount: Int = 0,
    val remainingCount: Int = 0,
    val keptCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val skippedCount: Int = 0
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

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0
          AND selfOrganizeStatus IN (0, 3)
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 selfOrganizeSkipOrder ASC,
                 CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
        LIMIT 1
    """)
    suspend fun getNextOrderedSelfOrganizePhoto(): MediaAsset?

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0
          AND selfOrganizeStatus IN (0, 3)
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 CASE WHEN selfOrganizeStatus = 3 THEN selfOrganizeSkipOrder END ASC,
                 CASE WHEN selfOrganizeStatus = 0 THEN RANDOM() END
        LIMIT 1
    """)
    suspend fun getNextRandomSelfOrganizePhoto(): MediaAsset?

    @Query("""
        SELECT COUNT(*) AS totalCount,
               COUNT(CASE WHEN selfOrganizeStatus IN (0, 3) THEN 1 END) AS remainingCount,
               COUNT(CASE WHEN selfOrganizeStatus = 1 THEN 1 END) AS keptCount,
               COUNT(CASE WHEN selfOrganizeStatus = 2 THEN 1 END) AS pendingDeleteCount,
               COUNT(CASE WHEN selfOrganizeStatus = 3 THEN 1 END) AS skippedCount
        FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0
    """)
    fun observeSelfOrganizeSummary(): Flow<SelfOrganizeSummary>

    @Query("SELECT EXISTS(SELECT 1 FROM self_organize_actions)")
    fun observeCanUndoSelfOrganize(): Flow<Boolean>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0
          AND selfOrganizeStatus = 2
        ORDER BY CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
    """)
    fun observeSelfOrganizePendingDelete(): Flow<List<MediaAsset>>

    @Transaction
    suspend fun cancelSelfOrganizePendingDelete(id: Long) {
        val asset = findById(id) ?: return
        if (!asset.isVisible || asset.mediaType != 1 || asset.isProtected || asset.selfOrganizeStatus != 2) return
        updateSelfOrganizeState(id, 0, 0L)
        deleteSelfOrganizeActionsForAsset(id)
    }

    @Query("DELETE FROM self_organize_actions WHERE mediaAssetId = :id")
    suspend fun deleteSelfOrganizeActionsForAsset(id: Long)

    @Query("SELECT * FROM media_assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MediaAsset?

    @Query("SELECT COALESCE(MAX(selfOrganizeSkipOrder), 0) + 1 FROM media_assets")
    suspend fun nextSelfOrganizeSkipOrder(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSelfOrganizeAction(action: SelfOrganizeActionRecord)

    @Query("SELECT * FROM self_organize_actions ORDER BY id DESC LIMIT 1")
    suspend fun getLastSelfOrganizeAction(): SelfOrganizeActionRecord?

    @Query("DELETE FROM self_organize_actions WHERE id = :id")
    suspend fun deleteSelfOrganizeAction(id: Long)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status, selfOrganizeSkipOrder = :skipOrder WHERE id = :id")
    suspend fun restoreSelfOrganizeState(id: Long, status: Int, skipOrder: Long)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status, selfOrganizeSkipOrder = :skipOrder WHERE id = :id")
    suspend fun updateSelfOrganizeState(id: Long, status: Int, skipOrder: Long)

    @Transaction
    suspend fun applySelfOrganizeDecision(id: Long, status: Int) {
        val current = findById(id) ?: return
        if (!current.isVisible || current.mediaType != 1 || current.isProtected) return
        insertSelfOrganizeAction(
            SelfOrganizeActionRecord(
                mediaAssetId = id,
                previousStatus = current.selfOrganizeStatus,
                previousSkipOrder = current.selfOrganizeSkipOrder,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        val skipOrder = if (status == 3) nextSelfOrganizeSkipOrder() else 0L
        updateSelfOrganizeState(id, status, skipOrder)
    }

    @Transaction
    suspend fun undoLastSelfOrganizeDecision(): MediaAsset? {
        val action = getLastSelfOrganizeAction() ?: return null
        val current = findById(action.mediaAssetId)
        if (current != null) {
            restoreSelfOrganizeState(
                id = action.mediaAssetId,
                status = action.previousStatus,
                skipOrder = action.previousSkipOrder
            )
        }
        deleteSelfOrganizeAction(action.id)
        return current?.copy(
            selfOrganizeStatus = action.previousStatus,
            selfOrganizeSkipOrder = action.previousSkipOrder
        )?.takeIf { it.isVisible && it.mediaType == 1 && !it.isProtected }
    }

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

    /** Hide trashed items without deleting their local metadata or user state. */
    @Query("UPDATE media_assets SET isVisible = 0 WHERE id IN (:ids)")
    suspend fun hideFromLibraryForIds(ids: List<Long>)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status, selfOrganizeSkipOrder = 0 WHERE id = :id")
    suspend fun setSelfOrganizeStatus(id: Long, status: Int)
}
