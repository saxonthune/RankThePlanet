// Dev-only: plain-text properties file at ~/.rtp/secure_store.properties.
// Not encrypted — JVM is not a production target (doc01.04.03 / CLAUDE.md).
package com.saxonthune.ranktheplanet.data.secure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

private class JvmSecureStore : SecureStore {
    private fun storeFile() = File(System.getProperty("user.home"), ".rtp/secure_store.properties")

    private fun load(): Properties {
        val props = Properties()
        val file = storeFile()
        if (file.exists()) file.inputStream().use { props.load(it) }
        return props
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        load().getProperty(key)
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        val props = load()
        props.setProperty(key, value)
        val file = storeFile()
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, null) }
    }
}

actual fun createSecureStore(): SecureStore = JvmSecureStore()
