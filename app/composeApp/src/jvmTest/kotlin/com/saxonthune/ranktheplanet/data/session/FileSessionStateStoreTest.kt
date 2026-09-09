package com.saxonthune.ranktheplanet.data.session

import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.domain.Viewport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FileSessionStateStoreTest {

    private fun tempDir() = Files.createTempDirectory("rtp-session-test").toFile().absolutePath

    // Use debounceMillis=0 so writes are flushed immediately (no virtual-time manipulation needed).
    // backgroundScope lets the infinite collect loop be cancelled cleanly at test end.
    private fun TestScope.store(dir: String) =
        FileSessionStateStore(backgroundScope, dataDir = dir, debounceMillis = 0)

    @Test
    fun defaultsWhenFileAbsent() = runTest {
        val s = store(tempDir())
        assertEquals(DEFAULT_VIEWPORT, s.load().viewport)
        assertNull(s.load().selectedPinEntryId)
    }

    @Test
    fun roundTripsViewport() = runTest {
        val dir = tempDir()
        val newViewport = Viewport(51.5, -0.12, 14.0, 0.0)

        val s = store(dir)
        s.saveViewport(newViewport)
        runCurrent() // flush the pending write coroutine

        val s2 = store(dir)
        assertEquals(newViewport, s2.load().viewport)
    }

    @Test
    fun roundTripsSelectedPin() = runTest {
        val dir = tempDir()
        val s = store(dir)
        s.saveSelectedPin(EntryId("entry-xyz"))
        runCurrent()

        val s2 = store(dir)
        assertEquals("entry-xyz", s2.load().selectedPinEntryId)
    }

    @Test
    fun clearSelectedPin() = runTest {
        val dir = tempDir()
        val s = store(dir)
        s.saveSelectedPin(EntryId("entry-xyz"))
        runCurrent()
        s.saveSelectedPin(null)
        runCurrent()

        val s2 = store(dir)
        assertNull(s2.load().selectedPinEntryId)
    }

    @Test
    fun corruptFileReturnsDefaults() = runTest {
        val dir = tempDir()
        writeFileText("$dir/session-state.json", "{ not valid json !!!")

        val s = store(dir)
        assertEquals(DEFAULT_VIEWPORT, s.load().viewport)
    }

    @Test
    fun observeViewportEmitsCurrentAndUpdates() = runTest {
        val s = store(tempDir())

        assertEquals(DEFAULT_VIEWPORT, s.observeViewport().first())

        val newViewport = Viewport(48.85, 2.35, 10.0, 0.0)
        s.saveViewport(newViewport)

        assertEquals(newViewport, s.observeViewport().first())
    }

    @Test
    fun rapidWritesCoalesceToLastValue() = runTest {
        val dir = tempDir()
        val s = store(dir)

        val viewports = (1..10).map { i -> Viewport(i.toDouble(), i.toDouble(), i.toDouble(), 0.0) }
        viewports.forEach { s.saveViewport(it) }
        // In-memory state reflects the latest value immediately (no delay needed).
        assertEquals(viewports.last(), s.load().viewport)

        runCurrent() // flush write
        val s2 = store(dir)
        assertEquals(viewports.last(), s2.load().viewport)
    }

    @Test
    fun debounceDelaysWrite() = runTest {
        val dir = tempDir()
        // Use real 500ms debounce with virtual time
        val s = FileSessionStateStore(backgroundScope, dataDir = dir, debounceMillis = 500L)

        s.saveViewport(Viewport(1.0, 1.0, 8.0, 0.0))
        // Before debounce completes — file not yet written
        val before = store(dir)
        assertEquals(DEFAULT_VIEWPORT, before.load().viewport)

        advanceTimeBy(600)
        val after = store(dir)
        assertEquals(Viewport(1.0, 1.0, 8.0, 0.0), after.load().viewport)
    }
}
