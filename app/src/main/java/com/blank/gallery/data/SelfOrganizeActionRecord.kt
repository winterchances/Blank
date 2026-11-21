package com.blank.gallery.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "self_organize_actions",
    indices = [Index(value = ["createdAtMillis"])]
)
data class SelfOrganizeActionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaAssetId: Long,
    val previousStatus: Int,
    val previousSkipOrder: Long,
    val createdAtMillis: Long
)
