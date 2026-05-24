package com.saxonthune.ranktheplanet.data.db

import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.SecureStoreKeys

suspend fun obtainOrCreatePassphrase(secureStore: SecureStore): String {
    secureStore.get(SecureStoreKeys.DB_PASSPHRASE)?.let { return it }
    val generated = generatePassphrase()
    secureStore.put(SecureStoreKeys.DB_PASSPHRASE, generated)
    return generated
}

private fun generatePassphrase(): String {
    // 32 random bytes hex-encoded — sufficient entropy for SQLCipher.
    val bytes = ByteArray(32)
    kotlin.random.Random.nextBytes(bytes)
    return bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
