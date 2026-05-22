package com.saxonthune.ranktheplanet.data.location

import com.saxonthune.ranktheplanet.data.fake.FakeLocationProvider
import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.SecureStoreKeys
import com.saxonthune.ranktheplanet.domain.SourceType
import io.ktor.client.HttpClient

interface LocationProviderRegistry {
    fun providerFor(type: SourceType): LocationProvider?
    fun default(): LocationProvider
    fun configured(): List<LocationProvider>
    fun setDefault(type: SourceType)
    fun addProvider(provider: LocationProvider)
    suspend fun saveProviderKey(type: SourceType, key: String)
}

class DefaultLocationProviderRegistry private constructor(
    private val secureStore: SecureStore,
    googleHttpClient: HttpClient,
) : LocationProviderRegistry {

    private val providers = mutableMapOf<SourceType, LocationProvider>()
    private var defaultType: SourceType = SourceType.Osm

    @kotlin.concurrent.Volatile private var googleApiKeySnapshot: String? = null

    init {
        providers[SourceType.Osm] = OsmLocationProvider(createOsmHttpClient())
        providers[SourceType.Google] = GoogleLocationProvider(
            client = googleHttpClient,
            apiKey = { googleApiKeySnapshot },
        )
        providers[SourceType.Fake] = FakeLocationProvider()
    }

    private suspend fun refreshGoogleApiKey() {
        googleApiKeySnapshot = secureStore.get(SecureStoreKeys.GOOGLE_PLACES_API_KEY)
            ?.takeIf { it.isNotBlank() }
    }

    // Only Google is a BYOK provider; other SourceTypes are no-ops.
    override suspend fun saveProviderKey(type: SourceType, key: String) {
        if (type == SourceType.Google) {
            secureStore.put(SecureStoreKeys.GOOGLE_PLACES_API_KEY, key)
            refreshGoogleApiKey()
        }
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

    companion object {
        suspend fun create(
            secureStore: SecureStore,
            googleHttpClient: HttpClient = createGoogleHttpClient(),
        ): DefaultLocationProviderRegistry {
            val registry = DefaultLocationProviderRegistry(secureStore, googleHttpClient)
            registry.refreshGoogleApiKey()
            return registry
        }
    }
}
