package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.SecureStoreKeys
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderRowUi(
    val type: SourceType,
    val displayName: String,
    val isDefault: Boolean,
    val isConfigured: Boolean,
)

data class ManageProvidersUiState(
    val defaultName: String = "",
    val rows: ImmutableList<ProviderRowUi> = persistentListOf(),
    val isLoading: Boolean = true,
)

class ManageProvidersViewModel(
    private val registry: LocationProviderRegistry,
    private val secureStore: SecureStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ManageProvidersUiState())
    val uiState: StateFlow<ManageProvidersUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val defaultProvider = registry.default()
            val configured = registry.configured()
            val hasGoogleKey = secureStore.get(SecureStoreKeys.GOOGLE_PLACES_API_KEY)?.isNotBlank() == true
            val rows = configured.map { provider ->
                ProviderRowUi(
                    type = provider.type,
                    displayName = providerDisplayName(provider.type),
                    isDefault = provider.type == defaultProvider.type,
                    isConfigured = when (provider.type) {
                        SourceType.Google -> hasGoogleKey
                        else -> true
                    },
                )
            }.toImmutableList()
            _state.update {
                it.copy(
                    defaultName = providerDisplayName(defaultProvider.type),
                    rows = rows,
                    isLoading = false,
                )
            }
        }
    }
}

internal fun providerDisplayName(type: SourceType): String = when (type) {
    SourceType.Osm -> "OpenStreetMap"
    SourceType.Google -> "Google Places"
    SourceType.Fake -> "Fake (dev)"
    else -> type.name
}
