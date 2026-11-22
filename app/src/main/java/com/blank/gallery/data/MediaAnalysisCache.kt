package com.blank.gallery.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local analysis results for a media asset.
 *
 * A cached hash is reusable only while the source size, modification time,
 * and hash algorithm version remain unchanged.
 */
@Entity(
    tableName = "media_analysis_cache",
    indices = [
        Index(value = ["sha256"]),
        Index(value = ["sourceSizeBytes", "sha256"])
    ]
)
data class MediaAnalysisCache(
    @PrimaryKey val mediaAssetId: Long,
    val sourceSizeBytes: Long,
    val sourceDateModifiedSeconds: Long,
    val sha256: String? = null,
    val perceptualHash: String? = null,
    val hashAlgorithmVersion: Int = SHA256_ALGORITHM_VERSION,
    val analyzedAtMillis: Long
) {
    companion object {
        const val SHA256_ALGORITHM_VERSION = 1
    }
}
