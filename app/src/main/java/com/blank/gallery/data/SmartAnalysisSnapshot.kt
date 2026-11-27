package com.blank.gallery.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "smart_analysis_snapshots",
    primaryKeys = ["analysisType"]
)
data class SmartAnalysisSnapshot(
    val analysisType: String,
    val parameterKey: String,
    val completedAtMillis: Long
)

@Entity(
    tableName = "smart_analysis_group_members",
    primaryKeys = ["analysisType", "groupKey", "mediaAssetId"],
    indices = [
        Index(value = ["analysisType", "groupKey"]),
        Index(value = ["mediaAssetId"])
    ]
)
data class SmartAnalysisGroupMember(
    val analysisType: String,
    val groupKey: String,
    val mediaAssetId: Long,
    val groupOrder: Int,
    val position: Int,
    val sourceContentUri: String,
    val sourceSizeBytes: Long,
    val sourceDateModifiedSeconds: Long
)

data class SmartAnalysisGroupAsset(
    val groupKey: String,
    val groupOrder: Int,
    val position: Int,
    @Embedded val asset: MediaAsset
)
