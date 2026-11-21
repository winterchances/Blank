package com.blank.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads only media currently visible through Android's granted MediaStore access.
 * Results are emitted in small batches so large libraries are not held in memory.
 */
class MediaStoreScanner(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun scanVisibleMedia(
        includeImages: Boolean,
        includeVideos: Boolean,
        modifiedSinceSeconds: Long?,
        onBatch: suspend (List<MediaAsset>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var total = 0
        if (includeImages) {
            total += queryCollection(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                mediaType = 1,
                durationColumn = null,
                modifiedSinceSeconds = modifiedSinceSeconds,
                onBatch = onBatch
            )
        }
        if (includeVideos) {
            total += queryCollection(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaType = 2,
                durationColumn = MediaStore.Video.Media.DURATION,
                modifiedSinceSeconds = modifiedSinceSeconds,
                onBatch = onBatch
            )
        }
        total
    }

    private suspend fun queryCollection(
        collection: android.net.Uri,
        mediaType: Int,
        durationColumn: String?,
        modifiedSinceSeconds: Long?,
        onBatch: suspend (List<MediaAsset>) -> Unit
    ): Int {
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

        var offset = 0
        var total = 0
        while (true) {
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, BATCH_SIZE)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                if (modifiedSinceSeconds != null) {
                    val modified = MediaStore.MediaColumns.DATE_MODIFIED
                    val added = MediaStore.MediaColumns.DATE_ADDED
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "$modified >= ? OR $added >= ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(modifiedSinceSeconds.toString(), modifiedSinceSeconds.toString())
                    )
                }
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns._ID)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
            }

            val batch = mutableListOf<MediaAsset>()
            val cursor = resolver.query(collection, projection, queryArgs, null)
                ?: throw IllegalStateException("MediaStore returned no cursor")
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = it.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val addedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val widthIndex = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightIndex = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationIndex = durationColumn?.let(cursor::getColumnIndex) ?: -1

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    batch += MediaAsset(
                        mediaStoreId = id,
                        contentUri = ContentUris.withAppendedId(collection, id).toString(),
                        displayName = it.getString(nameIndex) ?: "未命名",
                        mimeType = mimeIndex.takeIf { it >= 0 }?.let { cursor.getString(it) },
                        mediaType = mediaType,
                        sizeBytes = sizeIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                        dateAddedSeconds = addedIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                        dateModifiedSeconds = modifiedIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L,
                        width = widthIndex.takeIf { it >= 0 }?.let { it.getInt(it) } ?: 0,
                        height = heightIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0,
                        durationMs = durationIndex.takeIf { it >= 0 }?.let { cursor.getLong(it) } ?: 0L
                    )
                }
            }

            if (batch.isEmpty()) break
            onBatch(batch)
            total += batch.size
            offset += batch.size
            if (batch.size < BATCH_SIZE) break
        }
        return total
    }

    private companion object {
        const val BATCH_SIZE = 300
    }
}
