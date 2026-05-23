package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType

data class LocationCandidate(
    val coordinates: Coordinates,
    val displayName: String,
    val sourceType: SourceType,
    val sourceId: String?,
    val cachedMetadata: String?
)

sealed interface ProviderResult<out T> {
    data class Ok<T>(val value: T) : ProviderResult<T>
    data class Failed(val error: ProviderError) : ProviderResult<Nothing>
}

enum class ProviderError { NETWORK, RATE_LIMITED, NOT_CONFIGURED, PROVIDER_ERROR }

interface LocationProvider {
    val type: SourceType
    suspend fun resolve(query: String): ProviderResult<List<LocationCandidate>>
    suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>>
    suspend fun healthCheck(): ProviderResult<Unit>
}
