package com.blank.gallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaAsset::class, SelfOrganizeActionRecord::class, MediaAnalysisCache::class, ProtectedFolder::class, SmartAnalysisSnapshot::class, SmartAnalysisGroupMember::class],
    version = 11,
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS media_analysis_cache (
                        mediaAssetId INTEGER NOT NULL,
                        sourceSizeBytes INTEGER NOT NULL,
                        sourceDateModifiedSeconds INTEGER NOT NULL,
                        sha256 TEXT,
                        perceptualHash TEXT,
                        hashAlgorithmVersion INTEGER NOT NULL,
                        analyzedAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(mediaAssetId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_analysis_cache_sha256 ON media_analysis_cache (sha256)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_media_analysis_cache_sourceSizeBytes_sha256 ON media_analysis_cache (sourceSizeBytes, sha256)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_analysis_cache ADD COLUMN perceptualHashAlgorithmVersion INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE media_assets ADD COLUMN dateTakenMillis INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS protected_folders (
                        relativePath TEXT NOT NULL PRIMARY KEY
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS smart_analysis_snapshots (
                        analysisType TEXT NOT NULL PRIMARY KEY,
                        parameterKey TEXT NOT NULL,
                        completedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS smart_analysis_group_members (
                        analysisType TEXT NOT NULL,
                        groupKey TEXT NOT NULL,
                        mediaAssetId INTEGER NOT NULL,
                        groupOrder INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        sourceContentUri TEXT NOT NULL,
                        sourceSizeBytes INTEGER NOT NULL,
                        sourceDateModifiedSeconds INTEGER NOT NULL,
                        PRIMARY KEY(analysisType, groupKey, mediaAssetId)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_smart_analysis_group_members_analysisType_groupKey ON smart_analysis_group_members (analysisType, groupKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_smart_analysis_group_members_mediaAssetId ON smart_analysis_group_members (mediaAssetId)")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
