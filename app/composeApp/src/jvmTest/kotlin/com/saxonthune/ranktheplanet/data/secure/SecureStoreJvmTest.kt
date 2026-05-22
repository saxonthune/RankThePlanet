package com.saxonthune.ranktheplanet.data.secure

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecureStoreJvmTest {

    private lateinit var tempHome: File
    private lateinit var store: SecureStore

    @BeforeTest
    fun setup() {
        tempHome = Files.createTempDirectory("rtp-secure-store-test").toFile()
        System.setProperty("user.home", tempHome.absolutePath)
        store = createSecureStore()
    }

    @AfterTest
    fun teardown() {
        tempHome.deleteRecursively()
    }

    @Test
    fun `get returns null for unset key`() = runBlocking {
        assertNull(store.get("missing_key"))
    }

    @Test
    fun `put then get round-trips a value`() = runBlocking {
        store.put("my_key", "my_value")
        assertEquals("my_value", store.get("my_key"))
    }

    @Test
    fun `overwriting a key returns the new value`() = runBlocking {
        store.put("key", "first")
        store.put("key", "second")
        assertEquals("second", store.get("key"))
    }
}
