package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.data.fake.FakeLocationProvider
import com.saxonthune.ranktheplanet.domain.SourceType

interface LocationProviderRegistry {
    fun providerFor(type: SourceType): LocationProvider?
    fun default(): LocationProvider
    fun configured(): List<LocationProvider>
    fun setDefault(type: SourceType)
    fun addProvider(provider: LocationProvider)
}

class DefaultLocationProviderRegistry : LocationProviderRegistry {

    private val providers = mutableMapOf<SourceType, LocationProvider>()
    private var defaultType: SourceType = SourceType.Osm

    init {
        providers[SourceType.Osm] = OsmLocationProvider(createOsmHttpClient())
        providers[SourceType.Google] = GoogleLocationProvider(
            client = createGoogleHttpClient(),
            apiKey = { null },
        )
        providers[SourceType.Fake] = FakeLocationProvider()
    }

    override fun providerFor(type: SourceType): LocationProvider? = providers[type]

    override fun default(): LocationProvider = providers[defaultType]!!

    override fun configured(): List<LocationProvider> = providers.values.toList()

    override fun setDefault(type: SourceType) {
        if (providers.containsKey(type)) defaultType = type
    }

    override fun addProvider(provider: LocationProvider) {
        providers[provider.type] = provider
    }
}
