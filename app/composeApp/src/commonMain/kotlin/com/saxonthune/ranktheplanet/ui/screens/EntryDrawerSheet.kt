package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    onEditReview: (EntryId) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DebugSheetLabel("EntryDrawerSheet")
        Surface(
            onClick = { onViewCollection(entry.collectionId) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .background(entry.collectionColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(entry.collectionName, style = MaterialTheme.typography.titleSmall)
            }
        }

        HorizontalDivider()

        Surface(
            onClick = { onTapLocation() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(entry.locationName, style = MaterialTheme.typography.titleMedium)
            }
        }

        HorizontalDivider()

        Surface(
            onClick = { onEditReview(entry.entryId) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val reviewedLine = when {
                    entry.reviewed && entry.reviewedDate != null -> "Reviewed ${entry.reviewedDate}"
                    entry.reviewed -> "Reviewed"
                    else -> "Unreviewed"
                }
                Text(reviewedLine, style = MaterialTheme.typography.bodyLarge)
                if (entry.summaryPreview != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.summaryPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
