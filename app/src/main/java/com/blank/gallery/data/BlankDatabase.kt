package com.blank.gallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaAsset::class],
    version = 2,
    exportSchema = false
)
abstract class BlankDatabase : RoomDatabase() {
    abstract fun mediaAssetDao(): MediaAssetDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN isVisible INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_assets_isVisible ON media_assets (isVisible)"
                )
            }
        }

        @Volatile
        private var INSTANCE: BlankDatabase? = null

        fun getInstance(context: Context): BlankDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlankDatabase::class.java,
                    "blank-local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
