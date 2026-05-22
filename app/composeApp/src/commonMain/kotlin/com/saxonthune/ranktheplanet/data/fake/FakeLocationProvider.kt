package com.saxonthune.ranktheplanet.data.fake

import com.saxonthune.ranktheplanet.data.location.LocationCandidate
import com.saxonthune.ranktheplanet.data.location.LocationProvider
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.domain.Coordinates
import com.saxonthune.ranktheplanet.domain.SourceType

class FakeLocationProvider : LocationProvider {

    override val type = SourceType.Fake

    private val candidates = Fixtures.locations.map { location ->
        LocationCandidate(
            coordinates = location.coordinates,
            displayName = location.displayName,
            sourceType = location.sourceType,
            sourceId = location.sourceId,
            cachedMetadata = location.cachedMetadata,
        )
    }

    override suspend fun resolve(query: String): ProviderResult<List<LocationCandidate>> {
        if (query.isBlank()) return ProviderResult.Ok(emptyList())
        return ProviderResult.Ok(
            candidates.filter { it.displayName.contains(query, ignoreCase = true) }
        )
    }

    override suspend fun resolveNearby(coordinates: Coordinates): ProviderResult<List<LocationCandidate>> =
        ProviderResult.Ok(emptyList())
}
