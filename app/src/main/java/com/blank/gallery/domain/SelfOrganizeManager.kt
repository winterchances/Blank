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

    suspend fun nextPhoto(mode: SelfOrganizeMode): MediaAsset? =
        repository.nextSelfOrganizePhoto(mode == SelfOrganizeMode.RANDOM)

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
