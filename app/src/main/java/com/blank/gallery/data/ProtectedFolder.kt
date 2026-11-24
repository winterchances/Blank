package com.blank.gallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protected_folders")
data class ProtectedFolder(
    @PrimaryKey val relativePath: String
)
