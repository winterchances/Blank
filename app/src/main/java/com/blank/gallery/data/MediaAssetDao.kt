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

data class HomeOrganizeSummary(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val remainingCount: Int = 0,
    val keptCount: Int = 0,
    val pendingDeleteCount: Int = 0
)

@Dao
interface MediaAssetDao {
    @Query("SELECT COALESCE(MAX(id), 0) FROM media_assets")
    suspend fun getMaxMediaAssetId(): Long
    @Query("""
        SELECT COUNT(*) FROM media_assets
        INNER JOIN (
            SELECT sizeBytes
            FROM media_assets
            WHERE isVisible = 1 AND isProtected = 0 AND sizeBytes > 0 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
            GROUP BY sizeBytes
            HAVING COUNT(*) > 1
        ) AS candidate_sizes ON candidate_sizes.sizeBytes = media_assets.sizeBytes
        WHERE media_assets.isVisible = 1
          AND media_assets.isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND media_assets.sizeBytes > 0
          AND media_assets.id <= :maxId
    """)
    suspend fun countDuplicateCandidates(maxId: Long): Int

    @Query("""
        SELECT media_assets.* FROM media_assets
        INNER JOIN (
            SELECT sizeBytes
            FROM media_assets
            WHERE isVisible = 1 AND isProtected = 0 AND sizeBytes > 0 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
            GROUP BY sizeBytes
            HAVING COUNT(*) > 1
        ) AS candidate_sizes ON candidate_sizes.sizeBytes = media_assets.sizeBytes
        WHERE media_assets.isVisible = 1
          AND media_assets.isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND media_assets.sizeBytes > 0 AND media_assets.id <= :maxId
          AND (:hasCursor = 0 OR media_assets.sizeBytes < :lastSizeBytes
               OR (media_assets.sizeBytes = :lastSizeBytes AND media_assets.id > :lastId))
        ORDER BY media_assets.sizeBytes DESC, media_assets.id ASC
        LIMIT :limit
    """)
    suspend fun getDuplicateCandidates(
        limit: Int,
        maxId: Long,
        lastSizeBytes: Long,
        lastId: Long,
        hasCursor: Boolean
    ): List<MediaAsset>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND sizeBytes > 0 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)")
    suspend fun countSimilarPhotoCandidates(maxId: Long): Int

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND sizeBytes > 0 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND (:hasCursor = 0 OR
               CASE WHEN dateTakenMillis > 0 THEN dateTakenMillis ELSE dateTakenSeconds * 1000 END < :lastCaptureTimeMillis
               OR (CASE WHEN dateTakenMillis > 0 THEN dateTakenMillis ELSE dateTakenSeconds * 1000 END = :lastCaptureTimeMillis
                   AND dateAddedSeconds < :lastDateAddedSeconds)
               OR (CASE WHEN dateTakenMillis > 0 THEN dateTakenMillis ELSE dateTakenSeconds * 1000 END = :lastCaptureTimeMillis
                   AND dateAddedSeconds = :lastDateAddedSeconds AND id > :lastId))
        ORDER BY CASE
                     WHEN dateTakenMillis > 0 THEN dateTakenMillis
                     ELSE dateTakenSeconds * 1000
                 END DESC,
                 dateAddedSeconds DESC,
                 id ASC
        LIMIT :limit
    """)
    suspend fun getSimilarPhotoCandidates(
        limit: Int,
        maxId: Long,
        lastCaptureTimeMillis: Long,
        lastDateAddedSeconds: Long,
        lastId: Long,
        hasCursor: Boolean
    ): List<MediaAsset>

    @Query("""
        SELECT COUNT(*) FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND sizeBytes > 0 AND dateTakenMillis > 0 AND id <= :maxId
    """)
    suspend fun countBurstPhotoCandidates(maxId: Long): Int

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND sizeBytes > 0 AND dateTakenMillis > 0 AND id <= :maxId
          AND (:hasCursor = 0 OR dateTakenMillis > :lastDateTakenMillis
               OR (dateTakenMillis = :lastDateTakenMillis AND id > :lastId))
        ORDER BY dateTakenMillis ASC, id ASC
        LIMIT :limit
    """)
    suspend fun getBurstPhotoCandidates(
        limit: Int,
        maxId: Long,
        lastDateTakenMillis: Long,
        lastId: Long,
        hasCursor: Boolean
    ): List<MediaAsset>

    @Query("""
        SELECT COUNT(*) FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
    """)
    suspend fun countScreenshotCandidates(maxId: Long): Int

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND mediaType = 1 AND id <= :maxId AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND (:hasCursor = 0 OR dateAddedSeconds < :lastDateAddedSeconds
               OR (dateAddedSeconds = :lastDateAddedSeconds AND id > :lastId))
        ORDER BY dateAddedSeconds DESC, id ASC
        LIMIT :limit
    """)
    suspend fun getScreenshotCandidates(
        limit: Int,
        maxId: Long,
        lastDateAddedSeconds: Long,
        lastId: Long,
        hasCursor: Boolean
    ): List<MediaAsset>

    @Query("""
        SELECT COUNT(*) FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND mediaType = 1 AND sizeBytes >= :minimumSizeBytes AND id <= :maxId
    """)
    suspend fun countLargeMediaCandidates(minimumSizeBytes: Long, maxId: Long): Int

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND mediaType = 1 AND sizeBytes >= :minimumSizeBytes AND id <= :maxId
          AND (:hasCursor = 0 OR sizeBytes < :lastSizeBytes
               OR (sizeBytes = :lastSizeBytes AND dateAddedSeconds < :lastDateAddedSeconds)
               OR (sizeBytes = :lastSizeBytes AND dateAddedSeconds = :lastDateAddedSeconds AND id > :lastId))
        ORDER BY sizeBytes DESC, dateAddedSeconds DESC, id ASC
        LIMIT :limit
    """)
    suspend fun getLargeMediaCandidates(
        minimumSizeBytes: Long,
        limit: Int,
        maxId: Long,
        lastSizeBytes: Long,
        lastDateAddedSeconds: Long,
        lastId: Long,
        hasCursor: Boolean
    ): List<MediaAsset>

    @Query("SELECT * FROM media_assets WHERE id IN (:ids) AND isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)")
    suspend fun getVisibleUnprotectedMediaForIds(ids: List<Long>): List<MediaAsset>

    @Query("SELECT * FROM media_analysis_cache WHERE mediaAssetId = :mediaAssetId LIMIT 1")
    suspend fun getMediaAnalysisCache(mediaAssetId: Long): MediaAnalysisCache?

    @Query("SELECT * FROM media_assets WHERE id = :mediaAssetId LIMIT 1")
    suspend fun getMediaAssetForAnalysisCache(mediaAssetId: Long): MediaAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaAnalysisCacheRow(cache: MediaAnalysisCache)

    /**
     * Merge independently computed hashes atomically. Different analyzers may
     * run at the same time, so replacing the whole row can otherwise discard
     * a hash written by another analyzer. The source is checked against the
     * current media row so a late result from an obsolete scan cannot replace
     * a newer source's cache.
     */
    @Transaction
    suspend fun saveMediaAnalysisCache(cache: MediaAnalysisCache, expectedContentUri: String) {
        val currentAsset = getMediaAssetForAnalysisCache(cache.mediaAssetId) ?: return
        if (!currentAsset.isVisible ||
            currentAsset.contentUri != expectedContentUri ||
            currentAsset.sizeBytes != cache.sourceSizeBytes ||
            currentAsset.dateModifiedSeconds != cache.sourceDateModifiedSeconds
        ) return

        val existing = getMediaAnalysisCache(cache.mediaAssetId)
        val sameSource = existing != null &&
            existing.sourceSizeBytes == cache.sourceSizeBytes &&
            existing.sourceDateModifiedSeconds == cache.sourceDateModifiedSeconds

        upsertMediaAnalysisCacheRow(
            cache.copy(
                sha256 = cache.sha256 ?: existing?.takeIf { sameSource }?.sha256,
                perceptualHash = cache.perceptualHash
                    ?: existing?.takeIf { sameSource }?.perceptualHash,
                hashAlgorithmVersion = if (cache.sha256 != null) {
                    cache.hashAlgorithmVersion
                } else {
                    existing?.takeIf { sameSource }?.hashAlgorithmVersion
                        ?: cache.hashAlgorithmVersion
                },
                perceptualHashAlgorithmVersion = if (cache.perceptualHash != null) {
                    cache.perceptualHashAlgorithmVersion
                } else {
                    existing?.takeIf { sameSource }?.perceptualHashAlgorithmVersion
                        ?: cache.perceptualHashAlgorithmVersion
                },
                analyzedAtMillis = maxOf(cache.analyzedAtMillis, existing?.analyzedAtMillis ?: 0L)
            )
        )
    }
    @Query("SELECT * FROM media_assets WHERE isVisible = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeAll(): Flow<List<MediaAsset>>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1
          AND mediaType = 1
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

    @Query("SELECT relativePath FROM protected_folders")
    fun observeProtectedFolderPaths(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM protected_folders WHERE :relativePath = relativePath OR substr(:relativePath, 1, length(relativePath)) = relativePath)")
    suspend fun isMediaInProtectedFolder(relativePath: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addProtectedFolder(folder: ProtectedFolder)

    @Query("DELETE FROM protected_folders WHERE relativePath = :relativePath")
    suspend fun removeProtectedFolder(relativePath: String)

    @Query("""
        SELECT relativePath, COUNT(*) AS itemCount
        FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND relativePath != ''
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
        WHERE isVisible = 1 AND mediaType = 1
        GROUP BY monthKey
        ORDER BY monthKey DESC
    """)
    fun observeMonths(): Flow<List<MediaMonth>>

    @Query("SELECT * FROM media_assets WHERE isVisible = 1 AND mediaType = 1 AND isFavorite = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeFavorites(): Flow<List<MediaAsset>>

    @Query("SELECT * FROM media_assets WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 1 ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeProtectedMedia(): Flow<List<MediaAsset>>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 1")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_assets WHERE isVisible = 1 AND mediaType = 2")
    fun observeVideoCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets WHERE isVisible = 1")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets WHERE isVisible = 1 AND mediaType = 1")
    fun observePhotoBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets WHERE isVisible = 1 AND mediaType = 2")
    fun observeVideoBytes(): Flow<Long>

    @Query("""
        SELECT COUNT(*) AS totalCount,
               COUNT(CASE WHEN selfOrganizeStatus IN (1, 2) OR isFavorite = 1 OR isProtected = 1 THEN 1 END) AS completedCount,
               COUNT(CASE WHEN selfOrganizeStatus IN (0, 3) AND isFavorite = 0 AND isProtected = 0 AND isVisible = 1 THEN 1 END) AS remainingCount,
               COUNT(CASE WHEN selfOrganizeStatus = 1 OR isFavorite = 1 OR isProtected = 1 THEN 1 END) AS keptCount,
               COUNT(CASE WHEN selfOrganizeStatus = 2 THEN 1 END) AS pendingDeleteCount
        FROM media_assets
        WHERE mediaType = 1
          AND (isVisible = 1 OR selfOrganizeStatus = 2)
          AND (selfOrganizeStatus = 2 OR NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath))
    """)
    fun observeHomeOrganizeSummary(): Flow<HomeOrganizeSummary>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
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
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND selfOrganizeStatus IN (0, 3)
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 CASE WHEN selfOrganizeStatus = 3 THEN selfOrganizeSkipOrder END ASC,
                 CASE WHEN selfOrganizeStatus = 0 THEN RANDOM() END
        LIMIT 1
    """)
    suspend fun getNextRandomSelfOrganizePhoto(): MediaAsset?

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND selfOrganizeStatus IN (0, 3) AND id != :currentId
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 selfOrganizeSkipOrder ASC,
                 CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
        LIMIT 1
    """)
    suspend fun getNextOrderedSelfOrganizePreview(currentId: Long): MediaAsset?

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND selfOrganizeStatus IN (0, 3) AND id != :currentId
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 CASE WHEN selfOrganizeStatus = 3 THEN selfOrganizeSkipOrder END ASC,
                 CASE WHEN selfOrganizeStatus = 0 THEN RANDOM() END
        LIMIT 1
    """)
    suspend fun getNextRandomSelfOrganizePreview(currentId: Long): MediaAsset?

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND selfOrganizeStatus IN (0, 3) AND id != :currentId
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 selfOrganizeSkipOrder ASC,
                 CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
        LIMIT 3
    """)
    suspend fun getNextOrderedSelfOrganizePreviews(currentId: Long): List<MediaAsset>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0 AND NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath)
          AND selfOrganizeStatus IN (0, 3) AND id != :currentId
        ORDER BY CASE WHEN selfOrganizeStatus = 0 THEN 0 ELSE 1 END,
                 CASE WHEN selfOrganizeStatus = 3 THEN selfOrganizeSkipOrder END ASC,
                 CASE WHEN selfOrganizeStatus = 0 THEN RANDOM() END
        LIMIT 3
    """)
    suspend fun getNextRandomSelfOrganizePreviews(currentId: Long): List<MediaAsset>

    @Query("""
        SELECT COUNT(*) AS totalCount,
               COUNT(CASE WHEN selfOrganizeStatus IN (0, 3) THEN 1 END) AS remainingCount,
               COUNT(CASE WHEN selfOrganizeStatus = 1 THEN 1 END) AS keptCount,
               COUNT(CASE WHEN selfOrganizeStatus = 2 THEN 1 END) AS pendingDeleteCount,
               COUNT(CASE WHEN selfOrganizeStatus = 3 THEN 1 END) AS skippedCount
        FROM media_assets
        WHERE isVisible = 1 AND mediaType = 1 AND isProtected = 0
          AND (selfOrganizeStatus = 2 OR NOT EXISTS (SELECT 1 FROM protected_folders pf WHERE media_assets.relativePath = pf.relativePath OR substr(media_assets.relativePath, 1, length(pf.relativePath)) = pf.relativePath))
    """)
    fun observeSelfOrganizeSummary(): Flow<SelfOrganizeSummary>

    @Query("SELECT EXISTS(SELECT 1 FROM self_organize_actions)")
    fun observeCanUndoSelfOrganize(): Flow<Boolean>

    @Query("""
        SELECT * FROM media_assets
        WHERE isVisible = 1 AND mediaType IN (1, 2) AND isProtected = 0
          AND selfOrganizeStatus = 2
        ORDER BY CASE WHEN dateTakenSeconds > 0 THEN dateTakenSeconds ELSE dateAddedSeconds END DESC,
                 id DESC
    """)
    fun observeSelfOrganizePendingDelete(): Flow<List<MediaAsset>>

    @Transaction
    suspend fun cancelSelfOrganizePendingDelete(id: Long) {
        val asset = findById(id) ?: return
        if (!asset.isVisible || asset.mediaType !in 1..2 || asset.isProtected || asset.selfOrganizeStatus != 2) return
        val action = getLastSelfOrganizeActionForAsset(id)
        if (action != null) {
            restoreSelfOrganizeState(id, action.previousStatus, action.previousSkipOrder)
            deleteSelfOrganizeAction(action.id)
        } else {
            updateSelfOrganizeState(id, 0, 0L)
        }
    }

    @Query("SELECT * FROM media_assets WHERE id IN (:ids) AND isVisible = 1 AND isProtected = 0 AND mediaType IN (1, 2) AND selfOrganizeStatus = 2")
    suspend fun getVisibleUnprotectedPendingSelfOrganizePhotos(ids: List<Long>): List<MediaAsset>

    @Query("SELECT * FROM media_assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MediaAsset?

    @Query("SELECT COALESCE(MAX(selfOrganizeSkipOrder), 0) + 1 FROM media_assets")
    suspend fun nextSelfOrganizeSkipOrder(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSelfOrganizeAction(action: SelfOrganizeActionRecord)

    @Query("SELECT * FROM self_organize_actions ORDER BY id DESC LIMIT 1")
    suspend fun getLastSelfOrganizeAction(): SelfOrganizeActionRecord?

    @Query("SELECT * FROM self_organize_actions WHERE mediaAssetId = :mediaAssetId ORDER BY id DESC LIMIT 1")
    suspend fun getLastSelfOrganizeActionForAsset(mediaAssetId: Long): SelfOrganizeActionRecord?

    @Query("DELETE FROM self_organize_actions WHERE id = :id")
    suspend fun deleteSelfOrganizeAction(id: Long)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status, selfOrganizeSkipOrder = :skipOrder WHERE id = :id")
    suspend fun restoreSelfOrganizeState(id: Long, status: Int, skipOrder: Long)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status, selfOrganizeSkipOrder = :skipOrder WHERE id = :id")
    suspend fun updateSelfOrganizeState(id: Long, status: Int, skipOrder: Long)

    @Transaction
    suspend fun applySelfOrganizeDecision(id: Long, status: Int): Boolean {
        if (status !in 1..3) return false
        val current = findById(id) ?: return false
        if (!current.isVisible || current.mediaType !in 1..2 ||
            current.isProtected || current.isFavorite ||
            current.selfOrganizeStatus !in listOf(0, 3) ||
            isMediaInProtectedFolder(current.relativePath)
        ) return false
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
        return true
    }

    @Transaction
    suspend fun applySmartSelfOrganizeDecision(asset: MediaAsset, status: Int): Boolean {
        if (status !in 1..3) return false
        val current = findById(asset.id) ?: return false
        if (current.contentUri != asset.contentUri ||
            current.sizeBytes != asset.sizeBytes ||
            current.dateModifiedSeconds != asset.dateModifiedSeconds ||
            !current.isVisible || current.mediaType != 1 ||
            current.isProtected || current.isFavorite ||
            current.selfOrganizeStatus !in listOf(0, 3) ||
            isMediaInProtectedFolder(current.relativePath)
        ) return false
        insertSelfOrganizeAction(
            SelfOrganizeActionRecord(
                mediaAssetId = asset.id,
                previousStatus = current.selfOrganizeStatus,
                previousSkipOrder = current.selfOrganizeSkipOrder,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        val skipOrder = if (status == 3) nextSelfOrganizeSkipOrder() else 0L
        updateSelfOrganizeState(asset.id, status, skipOrder)
        return true
    }

    @Transaction
    suspend fun undoLastSelfOrganizeDecision(): MediaAsset? {
        val action = getLastSelfOrganizeAction() ?: return null
        val current = findById(action.mediaAssetId)
        val canRestore = current != null &&
            !current.isFavorite && !current.isProtected &&
            current.isVisible && current.mediaType == 1
        if (canRestore) {
            restoreSelfOrganizeState(
                id = action.mediaAssetId,
                status = action.previousStatus,
                skipOrder = action.previousSkipOrder
            )
        }
        deleteSelfOrganizeAction(action.id)
        return if (canRestore) current?.copy(
            selfOrganizeStatus = action.previousStatus,
            selfOrganizeSkipOrder = action.previousSkipOrder
        ) else null
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
            dateTakenMillis = :dateTakenMillis,
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
        dateTakenMillis: Long,
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
                dateTakenMillis = item.dateTakenMillis,
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

    @Query("""
        UPDATE media_assets SET
            selfOrganizeStatus = CASE WHEN :favorite = 1 OR isProtected = 1 THEN 1 ELSE selfOrganizeStatus END,
            selfOrganizeSkipOrder = CASE WHEN :favorite = 1 OR isProtected = 1 THEN 0 ELSE selfOrganizeSkipOrder END,
            isFavorite = :favorite
        WHERE id = :id
    """)
    suspend fun updateFavoriteState(id: Long, favorite: Boolean)

    @Transaction
    suspend fun setFavorite(id: Long, favorite: Boolean) {
        updateFavoriteState(id, favorite)
        deleteSelfOrganizeActionsForIds(listOf(id))
    }

    @Query("""
        UPDATE media_assets SET
            selfOrganizeStatus = CASE WHEN :protectedValue = 1 OR isFavorite = 1 THEN 1 ELSE selfOrganizeStatus END,
            selfOrganizeSkipOrder = CASE WHEN :protectedValue = 1 OR isFavorite = 1 THEN 0 ELSE selfOrganizeSkipOrder END,
            isProtected = :protectedValue
        WHERE id = :id
    """)
    suspend fun updateProtectedState(id: Long, protectedValue: Boolean)

    @Transaction
    suspend fun setProtected(id: Long, protectedValue: Boolean) {
        updateProtectedState(id, protectedValue)
        deleteSelfOrganizeActionsForIds(listOf(id))
    }

    @Query("""
        UPDATE media_assets SET
            selfOrganizeStatus = CASE WHEN :favorite = 1 OR isProtected = 1 THEN 1 ELSE selfOrganizeStatus END,
            selfOrganizeSkipOrder = CASE WHEN :favorite = 1 OR isProtected = 1 THEN 0 ELSE selfOrganizeSkipOrder END,
            isFavorite = :favorite
        WHERE id IN (:ids)
    """)
    suspend fun updateFavoriteStateForIds(ids: List<Long>, favorite: Boolean)

    @Transaction
    suspend fun setFavoriteForIds(ids: List<Long>, favorite: Boolean) {
        if (ids.isEmpty()) return
        updateFavoriteStateForIds(ids, favorite)
        deleteSelfOrganizeActionsForIds(ids)
    }

    @Query("""
        UPDATE media_assets SET
            selfOrganizeStatus = CASE WHEN :protectedValue = 1 OR isFavorite = 1 THEN 1 ELSE selfOrganizeStatus END,
            selfOrganizeSkipOrder = CASE WHEN :protectedValue = 1 OR isFavorite = 1 THEN 0 ELSE selfOrganizeSkipOrder END,
            isProtected = :protectedValue
        WHERE id IN (:ids)
    """)
    suspend fun updateProtectedStateForIds(ids: List<Long>, protectedValue: Boolean)

    @Transaction
    suspend fun setProtectedForIds(ids: List<Long>, protectedValue: Boolean) {
        if (ids.isEmpty()) return
        updateProtectedStateForIds(ids, protectedValue)
        deleteSelfOrganizeActionsForIds(ids)
    }

    /** Hide trashed items while retaining their explicit delete decision for progress accounting. */
    @Query("UPDATE media_assets SET isVisible = 0, selfOrganizeStatus = 2 WHERE id IN (:ids)")
    suspend fun hideFromLibraryForIds(ids: List<Long>)

    @Query("DELETE FROM self_organize_actions WHERE mediaAssetId IN (:ids)")
    suspend fun deleteSelfOrganizeActionsForIds(ids: List<Long>)

    @Transaction
    suspend fun finalizeTrashedMedia(ids: List<Long>) {
        if (ids.isEmpty()) return
        hideFromLibraryForIds(ids)
        deleteSelfOrganizeActionsForIds(ids)
    }

}
