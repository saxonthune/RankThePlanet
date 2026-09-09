package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.nav.MapMode
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationDraftSheetHost(
    draft: LocationDraftSheet.Open,
    mode: MapMode,
    nearbyCandidates: ImmutableList<NearbyCandidateUi>,
    isResolvingNearby: Boolean,
    collectionPicks: ImmutableList<CollectionPickRowUi>,
    onDismiss: () -> Unit,
    onCancelAdd: () -> Unit,
    onOpenAddToCollection: () -> Unit,
    onFindNearby: () -> Unit,
    onAdoptCandidate: (String) -> Unit,
    onKeepCoordinates: () -> Unit,
    onManualNameChange: (String) -> Unit,
    onBackToDraft: () -> Unit,
    onNewCollection: () -> Unit,
    onPickCollection: (CollectionId) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        when (draft.phase) {
            DraftPhase.Draft -> LocationDraftSheet(
                draft = draft,
                mode = mode,
                nearbyCandidates = nearbyCandidates,
                isResolvingNearby = isResolvingNearby,
                onDismiss = onDismiss,
                onCancelAdd = onCancelAdd,
                onAddToCollection = onOpenAddToCollection,
                onFindNearby = onFindNearby,
                onAdoptCandidate = onAdoptCandidate,
                onKeepCoordinates = onKeepCoordinates,
                onManualNameChange = onManualNameChange,
            )
            DraftPhase.AddToCollection -> AddLocationToCollectionSheet(
                draft = draft,
                collections = collectionPicks,
                preSelectedCollectionId = (mode as? MapMode.AddingToCollection)?.collectionId,
                onBack = onBackToDraft,
                onNewCollection = onNewCollection,
                onPickCollection = onPickCollection,
            )
        }
    }
}
