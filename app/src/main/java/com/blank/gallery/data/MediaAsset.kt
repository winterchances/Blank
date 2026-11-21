package com.blank.gallery.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local index for a media item exposed by Android MediaStore.
 * The content URI is kept locally; no media bytes or analysis data are uploaded.
 */
@Entity(
    tableName = "media_assets",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["dateAddedSeconds"]),
        Index(value = ["mediaType"]),
        Index(value = ["isProtected"]),
        Index(value = ["isVisible"])
    ]
)
data class MediaAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long,
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    /** 1 = image, 2 = video (MediaStore.Files file type convention). */
    val mediaType: Int,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    @ColumnInfo(defaultValue = "0") val dateTakenSeconds: Long = 0L,
    val dateModifiedSeconds: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isProtected: Boolean = false,
    /** 0 = not reviewed, 1 = kept, 2 = pending deletion, 3 = skipped. */
    val selfOrganizeStatus: Int = 0,
    /** Hidden when the current MediaStore permission scope no longer exposes this item. */
    @ColumnInfo(defaultValue = "1") val isVisible: Boolean = true,
    /** Scan token used to finalize visibility only after a complete scan. */
    @ColumnInfo(defaultValue = "''") val lastSeenScanToken: String = ""
)
