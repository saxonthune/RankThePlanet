package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.data.location.ProviderError
import com.saxonthune.ranktheplanet.data.location.ProviderResult
import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.SecureStoreKeys
import com.saxonthune.ranktheplanet.domain.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProviderMode {
    data object Osm : ProviderMode
    data object Google : ProviderMode
    data object Fake : ProviderMode
}

sealed interface ProviderTestState {
    data object Idle : ProviderTestState
    data object Testing : ProviderTestState
    data object Ok : ProviderTestState
    data class Error(val message: String) : ProviderTestState
}

data class ProviderConfigUiState(
    val mode: ProviderMode,
    val providerName: String,
    val statusText: String,
    val isConfigured: Boolean,
    val isDefault: Boolean,
    val keyDraft: String = "",
    val isSaving: Boolean = false,
    val testState: ProviderTestState = ProviderTestState.Idle,
)

class ProviderConfigViewModel(
    private val providerType: SourceType,
    private val registry: LocationProviderRegistry,
    private val secureStore: SecureStore,
) : ViewModel() {

    private val mode = modeFor(providerType)
    private val isCurrentDefault get() = registry.default().type == providerType

    private val _state = MutableStateFlow(
        ProviderConfigUiState(
            mode = mode,
            providerName = providerDisplayName(providerType),
            statusText = buildStatusText(mode, isConfigured = false, isDefault = isCurrentDefault),
            isConfigured = false,
            isDefault = isCurrentDefault,
        )
    )
    val uiState: StateFlow<ProviderConfigUiState> = _state

    init {
        viewModelScope.launch {
            val isConfigured = when (providerType) {
                SourceType.Google -> secureStore.get(SecureStoreKeys.GOOGLE_PLACES_API_KEY)?.isNotBlank() == true
                else -> true
            }
            _state.update {
                it.copy(
                    isConfigured = isConfigured,
                    isDefault = isCurrentDefault,
                    statusText = buildStatusText(mode, isConfigured, isCurrentDefault),
                )
            }
        }
        viewModelScope.launch {
            registry.defaultFlow.collect {
                val isDefault = isCurrentDefault
                _state.update { state ->
                    state.copy(
                        isDefault = isDefault,
                        statusText = buildStatusText(mode, state.isConfigured, isDefault),
                    )
                }
            }
        }
    }

    fun onKeyDraftChange(value: String) {
        _state.update { it.copy(keyDraft = value) }
    }

    fun onSaveKey() {
        val draft = _state.value.keyDraft
        if (draft.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            registry.saveProviderKey(SourceType.Google, draft)
            val isConfigured = secureStore.get(SecureStoreKeys.GOOGLE_PLACES_API_KEY)?.isNotBlank() == true
            _state.update {
                it.copy(
                    isConfigured = isConfigured,
                    statusText = buildStatusText(mode, isConfigured, isCurrentDefault),
                    keyDraft = "",
                    isSaving = false,
                    testState = ProviderTestState.Idle,
                )
            }
        }
    }

    fun onTestConnection() {
        val provider = registry.providerFor(providerType) ?: return
        viewModelScope.launch {
            _state.update { it.copy(testState = ProviderTestState.Testing) }
            val result = provider.healthCheck()
            val testState = when (result) {
                is ProviderResult.Ok -> ProviderTestState.Ok
                is ProviderResult.Failed -> ProviderTestState.Error(errorMessage(result.error))
            }
            _state.update { it.copy(testState = testState) }
        }
    }

    fun onSetAsDefault() {
        registry.setDefault(providerType)
    }
}

private fun modeFor(type: SourceType): ProviderMode = when (type) {
    SourceType.Osm -> ProviderMode.Osm
    SourceType.Google -> ProviderMode.Google
    else -> ProviderMode.Fake
}

private fun errorMessage(error: ProviderError): String = when (error) {
    ProviderError.NOT_CONFIGURED -> "Not configured. Save a key first."
    ProviderError.NETWORK -> "Network error. Check your connection."
    ProviderError.RATE_LIMITED -> "Rate limited. Try again shortly."
    ProviderError.PROVIDER_ERROR -> "Provider rejected the request. Check the key, restrictions, and that Places API (New) is enabled."
}

private fun buildStatusText(mode: ProviderMode, isConfigured: Boolean, isDefault: Boolean): String =
    when (mode) {
        ProviderMode.Osm -> if (isDefault)
            "Default. Resolving with the public Photon and Nominatim endpoints."
        else
            "Available. Public Photon and Nominatim endpoints."
        ProviderMode.Google -> when {
            !isConfigured -> "Not configured. Paste a Places API (New) key to enable."
            isDefault -> "Default. Resolving via Google Places API (New)."
            else -> "Configured. Tap 'Set as default' to start using it."
        }
        ProviderMode.Fake -> "Dev provider. In-memory fixtures only."
    }
