package com.saxonthune.ranktheplanet.data.session

import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Viewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FileSessionStateStore(
    private val scope: CoroutineScope,
    dataDir: String = appDataDir(),
    private val debounceMillis: Long = 500L,
) : SessionStateStore {

    private val filePath = "$dataDir/session-state.json"

    // Load synchronously at construction time — constructor runs in a background coroutine
    // (inside produceState in MainActivity / MainViewController), so it never blocks the UI.
    private val _state = MutableStateFlow(
        try {
            readFileText(filePath)?.let { Json.decodeFromString<SessionState>(it) }
        } catch (_: Exception) {
            null
        } ?: SessionState()
    )

    // CONFLATED channel: holds at most 1 pending write signal; rapid saves coalesce into one flush.
    // Unlike MutableSharedFlow, a Channel buffers items even before the receiver starts collecting.
    private val _pendingWrite = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (unit in _pendingWrite) {
                if (debounceMillis > 0) delay(debounceMillis)
                try {
                    writeFileText(filePath, Json.encodeToString(_state.value))
                } catch (_: Exception) { /* best-effort */ }
            }
        }
    }

    override suspend fun load(): SessionState = _state.value

    override fun observeViewport(): Flow<Viewport> = _state.map { it.viewport }

    override suspend fun saveViewport(viewport: Viewport) {
        _state.updateAndGet { it.copy(viewport = viewport) }
        _pendingWrite.trySend(Unit)
    }

    override suspend fun saveSelectedPin(entryId: EntryId?) {
        _state.updateAndGet { it.copy(selectedPinEntryId = entryId?.value) }
        _pendingWrite.trySend(Unit)
    }
}
