package com.saxonthune.ranktheplanet.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saxonthune.ranktheplanet.data.CollectionPortIoService
import com.saxonthune.ranktheplanet.data.ImportProgress
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import com.saxonthune.ranktheplanet.io.FilePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class ImportFlowUiState(
    val phase: Phase = Phase.Idle,
    val pickedFileName: String? = null,
    val error: String? = null,
    val completedEntries: Int = 0,
    val totalEntries: Int = 0,
) {
    enum class Phase { Idle, Picking, Importing, Done }
}

sealed interface ImportFlowEvent {
    data class Imported(val collectionId: CollectionId) : ImportFlowEvent
    data object Cancelled : ImportFlowEvent
}

class ImportFlowViewModel(
    private val portIo: CollectionPortIoService,
    private val filePicker: FilePicker,
) : ViewModel() {

    private var importJob: Job? = null

    private val _uiState = MutableStateFlow(ImportFlowUiState())
    val uiState: StateFlow<ImportFlowUiState> = _uiState

    private val _events = Channel<ImportFlowEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPickFile() {
        if (importJob?.isActive == true) return
        importJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(phase = ImportFlowUiState.Phase.Picking, error = null)

            val pickResult = filePicker.openForRead(listOf(PortFormat.Kml, PortFormat.GeoJson))

            pickResult.onFailure { t ->
                val isCancel = t is CancellationException
                _uiState.value = _uiState.value.copy(
                    phase = ImportFlowUiState.Phase.Idle,
                    error = if (isCancel) null else (t.message ?: "Failed to pick file"),
                )
                return@launch
            }

            val picked = pickResult.getOrThrow()
            val format = picked.inferredFormat
            if (format == null) {
                _uiState.value = _uiState.value.copy(
                    phase = ImportFlowUiState.Phase.Idle,
                    error = "Unsupported file type. Pick a .kml or .geojson file.",
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                phase = ImportFlowUiState.Phase.Importing,
                pickedFileName = picked.name,
            )

            portIo.import(picked.text, format) { progress: ImportProgress ->
                _uiState.value = _uiState.value.copy(
                    completedEntries = progress.completed,
                    totalEntries = progress.total,
                )
            }.fold(
                onSuccess = { collectionId ->
                    _uiState.value = _uiState.value.copy(phase = ImportFlowUiState.Phase.Done)
                    _events.send(ImportFlowEvent.Imported(collectionId))
                },
                onFailure = { t ->
                    _uiState.value = _uiState.value.copy(
                        phase = ImportFlowUiState.Phase.Idle,
                        error = t.message ?: "Import failed",
                    )
                },
            )
        }
    }

    fun onCancel() {
        importJob?.cancel()
        viewModelScope.launch { _events.send(ImportFlowEvent.Cancelled) }
    }
}
