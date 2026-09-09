package com.saxonthune.ranktheplanet.data.session

import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Viewport
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

val DEFAULT_VIEWPORT = Viewport(
    centerLat = 40.7580,
    centerLng = -73.9855,
    zoom = 12.0,
    bearing = 0.0,
)

@Serializable
data class SessionState(
    val viewport: Viewport = DEFAULT_VIEWPORT,
    val selectedPinEntryId: String? = null,
)

interface SessionStateStore {
    suspend fun load(): SessionState
    fun observeViewport(): Flow<Viewport>
    suspend fun saveViewport(viewport: Viewport)
    suspend fun saveSelectedPin(entryId: EntryId?)
}
