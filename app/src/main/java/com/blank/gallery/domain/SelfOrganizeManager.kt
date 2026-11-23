package com.blank.gallery.domain

import com.blank.gallery.data.MediaAsset
import com.blank.gallery.data.MediaLibraryRepository
import com.blank.gallery.data.SelfOrganizeSummary
import com.blank.gallery.data.SelfOrganizeStatus
import kotlinx.coroutines.flow.Flow

enum class SelfOrganizeMode {
    ORDERED,
    RANDOM
}

enum class SelfOrganizeDecision {
    KEEP,
    PENDING_DELETE,
    SKIP
}

/** Coordinates the shared, persistent decision queue for both self-organize modes. */
class SelfOrganizeManager(
    private val repository: MediaLibraryRepository
) {
    val summary: Flow<SelfOrganizeSummary> = repository.selfOrganizeSummary
    val canUndo: Flow<Boolean> = repository.canUndoSelfOrganize
    val pendingDelete: Flow<List<MediaAsset>> = repository.selfOrganizePendingDelete

    suspend fun cancelPendingDelete(assetId: Long) {
        repository.cancelSelfOrganizePendingDelete(assetId)
    }

    suspend fun nextPhoto(mode: SelfOrganizeMode): MediaAsset? =
        repository.nextSelfOrganizePhoto(mode == SelfOrganizeMode.RANDOM)

    suspend fun peekNextPhoto(mode: SelfOrganizeMode, currentId: Long): MediaAsset? =
        repository.peekNextSelfOrganizePhoto(mode == SelfOrganizeMode.RANDOM, currentId)

    suspend fun applyDecision(assetId: Long, decision: SelfOrganizeDecision) {
        val status = when (decision) {
            SelfOrganizeDecision.KEEP -> SelfOrganizeStatus.KEPT
            SelfOrganizeDecision.PENDING_DELETE -> SelfOrganizeStatus.PENDING_DELETE
            SelfOrganizeDecision.SKIP -> SelfOrganizeStatus.SKIPPED
        }
        repository.applySelfOrganizeDecision(assetId, status)
    }

    suspend fun undo(): MediaAsset? = repository.undoLastSelfOrganizeDecision()
}
