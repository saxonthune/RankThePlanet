package com.saxonthune.ranktheplanet.data.projection

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.db.AppDatabase
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.MapOverviewState
import com.saxonthune.ranktheplanet.domain.VisiblePin
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PassthroughOverviewProjection(
    private val database: AppDatabase,
    private val sessionState: SessionStateStore,
) : OverviewProjection {

    override suspend fun loadOverview(): MapOverviewState {
        val pinRows = database.overviewQueries.visibleEntries().executeAsList()
        val collectionIds = database.overviewQueries.visibleCollectionIds().executeAsList()
        val viewport = sessionState.load().viewport
        return MapOverviewState(
            viewport = viewport,
            visiblePins = pinRows.map { row ->
                VisiblePin(
                    entryId = EntryId(row.entry_id),
                    coordinates = Coordinates(lat = row.lat, lng = row.lng),
                    collectionColor = row.appearance_color,
                    visited = row.visited != 0L,
                )
            }.toImmutableList(),
            collectionFilter = collectionIds.map { CollectionId(it) }.toImmutableSet(),
        )
    }

    override fun observeOverview(): Flow<MapOverviewState> = combine(
        database.overviewQueries.visibleEntries().asFlow().mapToList(Dispatchers.Default),
        database.overviewQueries.visibleCollectionIds().asFlow().mapToList(Dispatchers.Default),
        sessionState.observeViewport(),
    ) { pinRows, collectionIdRows, viewport ->
        MapOverviewState(
            viewport = viewport,
            visiblePins = pinRows.map { row ->
                VisiblePin(
                    entryId = EntryId(row.entry_id),
                    coordinates = Coordinates(lat = row.lat, lng = row.lng),
                    collectionColor = row.appearance_color,
                    visited = row.visited != 0L,
                )
            }.toImmutableList(),
            collectionFilter = collectionIdRows.map { CollectionId(it) }.toImmutableSet(),
        )
    }
}
