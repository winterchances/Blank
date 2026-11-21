package com.blank.gallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaAsset::class, SelfOrganizeActionRecord::class],
    version = 6,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN lastSeenScanToken TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN dateTakenSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN relativePath TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_assets_relativePath ON media_assets (relativePath)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN selfOrganizeSkipOrder INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS self_organize_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaAssetId INTEGER NOT NULL,
                        previousStatus INTEGER NOT NULL,
                        previousSkipOrder INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_self_organize_actions_createdAtMillis ON self_organize_actions (createdAtMillis)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
