package com.blank.gallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets ORDER BY dateAddedSeconds DESC, id DESC")
    fun observeAll(): Flow<List<MediaAsset>>

    @Query("SELECT COUNT(*) FROM media_assets WHERE mediaType = 1")
    fun observePhotoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_assets WHERE mediaType = 2")
    fun observeVideoCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM media_assets")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT * FROM media_assets WHERE mediaType = 1 AND selfOrganizeStatus = 0 AND isProtected = 0 ORDER BY dateAddedSeconds DESC, id DESC")
    suspend fun getNextSelfOrganizePhoto(): MediaAsset?

    @Query("SELECT * FROM media_assets WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun findByMediaStoreId(mediaStoreId: Long): MediaAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaAsset>)

    @Update
    suspend fun update(item: MediaAsset)

    @Query("DELETE FROM media_assets WHERE mediaStoreId NOT IN (:existingIds)")
    suspend fun deleteMissing(existingIds: List<Long>)

    @Query("DELETE FROM media_assets")
    suspend fun clearAll()

    @Query("UPDATE media_assets SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_assets SET isProtected = :protected WHERE id = :id")
    suspend fun setProtected(id: Long, protected: Boolean)

    @Query("UPDATE media_assets SET selfOrganizeStatus = :status WHERE id = :id")
    suspend fun setSelfOrganizeStatus(id: Long, status: Int)
}
