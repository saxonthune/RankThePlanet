package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.LocationRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.domain.Appearance
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.LocationId
import com.saxonthune.ranktheplanet.domain.ReviewDraft
import com.saxonthune.ranktheplanet.domain.SourceType
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import com.saxonthune.ranktheplanet.ui.RtpDrillDownScaffold
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit,
    collections: CollectionRepository,
    templates: TemplateRepository,
    locations: LocationRepository,
) {
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    RtpDrillDownScaffold(
        title = "Debug",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DebugRow(
                label = "Generate sample collection",
                subtitle = "Create a new NYC Geo Diary with stock entries",
                onClick = {
                    scope.launch {
                        val msg = runCatching {
                            val name = generateSampleNycCollection(collections, templates, locations)
                            "Created \"$name\""
                        }.getOrElse { "Failed: ${it.message}" }
                        snackbarHost.showSnackbar(msg, duration = SnackbarDuration.Short)
                    }
                },
            )
        }
    }
}

@Composable
private fun DebugRow(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class SampleLoc(
    val lat: Double,
    val lng: Double,
    val displayName: String,
    val sourceId: String,
    val description: String,
    val visitedOn: String,
)

private val sampleNycLocations = listOf(
    SampleLoc(40.7829, -73.9654, "Central Park — The Mall", "osm-4453738",
        "Read on a bench by The Mall while the buskers set up.", "2024-04-12"),
    SampleLoc(40.7480, -74.0048, "The High Line", "osm-4504115",
        "Walked the whole High Line at golden hour.", "2024-05-03"),
    SampleLoc(40.6602, -73.9690, "Prospect Park — Long Meadow", "osm-7773888",
        "Picnic on Long Meadow with friends visiting from out of town.", "2024-06-22"),
    SampleLoc(40.7144, -73.9680, "Domino Park", "osm-558129000",
        "Coffee by the water watching the ferries cross to Manhattan.", "2024-07-09"),
)

private suspend fun generateSampleNycCollection(
    collections: CollectionRepository,
    templates: TemplateRepository,
    locations: LocationRepository,
): String {
    val suffix = kotlin.random.Random.nextInt(0x1000, 0xFFFF).toString(16)
    val name = "Sample NYC Diary $suffix"

    val collection = collections.create(
        name = name,
        description = "Generated from debug settings.",
        appearance = Appearance(color = "#2E86AB", pinStyle = "pin"),
        powerRanking = false,
    ).getOrThrow()

    val template = templates.define(
        collectionId = collection.id,
        fields = listOf(
            TemplateField("description", "Description", FieldType.Text, TemplateFieldConfig.TextField, ordinal = 0),
            TemplateField("visitedOn", "Visited on", FieldType.Date, TemplateFieldConfig.Date, ordinal = 1),
        ),
    ).getOrThrow()

    for (s in sampleNycLocations) {
        val loc = Location(
            id = LocationId(""),
            coordinates = Coordinates(s.lat, s.lng),
            displayName = s.displayName,
            sourceType = SourceType.Osm,
            sourceId = s.sourceId,
            cachedMetadata = null,
            refreshable = true,
        )
        val persisted = locations.upsert(loc).getOrThrow()
        collections.addEntry(
            collectionId = collection.id,
            location = persisted,
            review = ReviewDraft(
                data = persistentMapOf(
                    "description" to s.description,
                    "visitedOn" to s.visitedOn,
                ),
                templateVersion = template.version,
            ),
        ).getOrThrow()
    }

    return name
}
