package com.saxonthune.ranktheplanet.data.projection

import com.saxonthune.ranktheplanet.domain.MapOverviewState
import kotlinx.coroutines.flow.Flow

interface OverviewProjection {
    /** Synchronous-feeling snapshot for the first frame after cold start. */
    suspend fun loadOverview(): MapOverviewState

    /** Live updates as the underlying store changes. */
    fun observeOverview(): Flow<MapOverviewState>
}
