package com.blank.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads only items currently visible through the user's MediaStore permissions.
 * It never opens or uploads media contents.
 */
class MediaStoreScanner(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun scanVisibleMedia(): List<MediaAsset> = withContext(Dispatchers.IO) {
        buildList {
            addAll(queryImages())
            addAll(queryVideos())
        }.sortedByDescending { it.dateAddedSeconds }
    }

    private fun queryImages(): List<MediaAsset> =
        queryCollection(
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = 1,
            durationColumn = null
        )

    private fun queryVideos(): List<MediaAsset> =
        queryCollection(
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = 2,
            durationColumn = MediaStore.Video.Media.DURATION
        )

    private fun queryCollection(
        collection: android.net.Uri,
        mediaType: Int,
        durationColumn: String?
    ): List<MediaAsset> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            durationColumn?.let(::add)
        }.toTypedArray()

        val result = mutableListOf<MediaAsset>()
        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthIndex = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationIndex = durationColumn?.let(cursor::getColumnIndex) ?: -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                result += MediaAsset(
                    mediaStoreId = id,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = cursor.getString(nameIndex) ?: "未命名",
                    mimeType = mimeIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
                    mediaType = mediaType,
                    sizeBytes = sizeIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                    dateAddedSeconds = addedIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                    dateModifiedSeconds = modifiedIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                    width = widthIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0,
                    height = heightIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0,
                    durationMs = durationIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L
                )
            }
        }
        return result
    }
}
