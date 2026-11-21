package com.blank.gallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaAsset::class],
    version = 1,
    exportSchema = false
)
abstract class BlankDatabase : RoomDatabase() {
    abstract fun mediaAssetDao(): MediaAssetDao

    companion object {
        @Volatile
        private var INSTANCE: BlankDatabase? = null

        fun getInstance(context: Context): BlankDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlankDatabase::class.java,
                    "blank-local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
