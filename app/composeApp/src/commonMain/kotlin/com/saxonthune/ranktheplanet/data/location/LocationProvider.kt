package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType

data class LocationCandidate(
    val coordinates: Coordinates,
    val displayName: String,
    val sourceType: SourceType,
    val sourceId: String?,
    val cachedMetadata: String?,
    val detail: String? = null,
    val needsConfirmation: Boolean = false,
)

sealed interface LocationBias {
    data class Point(val coordinates: Coordinates) : LocationBias
    data class Box(
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
    ) : LocationBias
}

sealed interface ProviderResult<out T> {
    data class Ok<T>(val value: T) : ProviderResult<T>
    data class Failed(val error: ProviderError) : ProviderResult<Nothing>
}

enum class ProviderError { NETWORK, RATE_LIMITED, NOT_CONFIGURED, PROVIDER_ERROR }

interface LocationProvider {
    val type: SourceType
    val supportsTypeahead: Boolean

    /**
     * How long the search field waits after the last keystroke before issuing a
     * typeahead `resolve`. A per-provider trait because the right value follows the
     * backing service's cost/rate model — a politeness throttle, per-call billing, or
     * session-coalescing all push it differently. Ignored when [supportsTypeahead] is
     * false (those providers query only on submit). Read by the search field both to
     * pace the query and to drive the countdown ring that shows the pending window.
     */
    val typeaheadDebounceMillis: Int get() = 300

    suspend fun resolve(query: String, bias: LocationBias? = null): ProviderResult<List<LocationCandidate>>
    suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>>
    suspend fun healthCheck(): ProviderResult<Unit>
    suspend fun confirm(candidate: LocationCandidate): ProviderResult<LocationCandidate> =
        ProviderResult.Ok(candidate)
}
