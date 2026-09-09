package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId

@Composable
internal fun EntryDrawerSheet(
    entry: EntrySummaryUi,
    onTapLocation: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onJumpToCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        SheetLabel("Review")
        CollectionBreadcrumb(
            entry = entry,
            onViewCollectionClick = { onViewCollection(entry.collectionId) },
            onJumpClick = { onJumpToCollection(entry.collectionId) },
        )
        LocationTitle(
            entry = entry,
            onClick = onTapLocation,
        )
        ReviewRow(
            entry = entry,
            onClick = { onEditReview(entry.entryId) },
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CollectionBreadcrumb(
    entry: EntrySummaryUi,
    onViewCollectionClick: () -> Unit,
    onJumpClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onViewCollectionClick),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(entry.collectionColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.collectionName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onJumpClick) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "See on Map",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationTitle(
    entry: EntrySummaryUi,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 20.dp),
    ) {
        Text(
            text = entry.locationName,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.locationAddress != null) {
            Text(
                text = entry.locationAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReviewRow(
    entry: EntrySummaryUi,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val statusLine = entry.reviewedDate
                ?.let { "Reviewed $it" }
                ?: "Unreviewed"
            Text(
                text = statusLine,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (entry.summaryPreview != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.summaryPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit review",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
