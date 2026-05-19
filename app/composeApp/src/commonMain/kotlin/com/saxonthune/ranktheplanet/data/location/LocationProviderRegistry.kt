package com.saxonthune.ranktheplanet.data.location

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
    }

    override fun providerFor(type: SourceType): LocationProvider? = providers[type]

    override fun default(): LocationProvider = providers[defaultType]!!

    override fun configured(): List<LocationProvider> = providers.values.toList()

    override fun setDefault(type: SourceType) {
        if (providers.containsKey(type)) defaultType = type
    }

    override fun addProvider(provider: LocationProvider) {
        // BYOK providers (Google, Apple, Mapbox) are not yet supported
        if (provider.type == SourceType.Osm) {
            providers[provider.type] = provider
        }
    }
}
