package com.blank.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads media from the merged external-storage volume exposed by MediaStore.
 * Rows are streamed to Room in small batches so large libraries are not held
 * in memory and do not depend on provider-specific keyset pagination behavior.
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
                collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                mediaType = 1,
                durationColumn = null,
                modifiedSinceSeconds = modifiedSinceSeconds,
                onBatch = onBatch
            )
        }
        if (includeVideos) {
            total += queryCollection(
                collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
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
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            durationColumn?.let(::add)
        }.toTypedArray()

        val queryArgs = Bundle().apply {
            if (modifiedSinceSeconds != null) {
                val modified = MediaStore.MediaColumns.DATE_MODIFIED
                val added = MediaStore.MediaColumns.DATE_ADDED
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "($modified >= ? OR $added >= ?)"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(
                        modifiedSinceSeconds.toString(),
                        modifiedSinceSeconds.toString()
                    )
                )
            }
        }

        var total = 0
        val batch = ArrayList<MediaAsset>(BATCH_SIZE)
        val cursor = resolver.query(collection, projection, queryArgs, null)
            ?: throw IllegalStateException("MediaStore returned no cursor")

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val pathIndex = c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val mimeIndex = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val addedIndex = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val takenIndex = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val modifiedIndex = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthIndex = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationIndex = durationColumn?.let(c::getColumnIndex) ?: -1

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val addedSeconds = addedIndex.takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L
                val takenMillis = takenIndex.takeIf { it >= 0 }
                    ?.let { c.getLong(it).takeIf { value -> value > 0L } }
                    ?: 0L
                val takenSeconds = if (takenMillis > 0L) {
                    takenMillis / 1000L
                } else {
                    addedSeconds
                }
                batch += MediaAsset(
                    mediaStoreId = id,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    displayName = c.getString(nameIndex) ?: "未命名",
                    relativePath = pathIndex.takeIf { it >= 0 }?.let { c.getString(it) }.orEmpty(),
                    mimeType = mimeIndex.takeIf { it >= 0 }?.let { c.getString(it) },
                    mediaType = mediaType,
                    sizeBytes = sizeIndex.takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L,
                    dateAddedSeconds = addedSeconds,
                    dateTakenSeconds = takenSeconds,
                    dateTakenMillis = takenMillis,
                    dateModifiedSeconds = modifiedIndex.takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L,
                    width = widthIndex.takeIf { it >= 0 }?.let { c.getInt(it) } ?: 0,
                    height = heightIndex.takeIf { it >= 0 }?.let { c.getInt(it) } ?: 0,
                    durationMs = durationIndex.takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L
                )

                if (batch.size == BATCH_SIZE) {
                    onBatch(batch.toList())
                    total += batch.size
                    batch.clear()
                }
            }
        }

        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            total += batch.size
        }
        return total
    }

    private companion object {
        const val BATCH_SIZE = 300
    }
}
