package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinSheetHost(
    sheet: PinSheet,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPickEntryFromPeek: (EntryId) -> Unit,
    onAddEntryAtPeek: () -> Unit,
    onConfirmAddAtPeek: () -> Unit,
    onTapEntryLocation: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
) {
    if (sheet is PinSheet.None) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // Sheet's containerColor extends through the bottom safe area; sheet content
        // applies its own navigationBarsPadding to clear the system gesture inset.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        when (sheet) {
            is PinSheet.Peek -> LocationDetailPeek(
                peek = sheet,
                onPickEntry = onPickEntryFromPeek,
                onAddEntry = onAddEntryAtPeek,
                onConfirmAdd = onConfirmAddAtPeek,
            )
            is PinSheet.Entry -> EntryDrawerSheet(
                entry = sheet.entry,
                onTapLocation = onTapEntryLocation,
                onViewCollection = onViewCollection,
                onEditReview = onEditReview,
            )
            is PinSheet.None -> {}
        }
    }
}
