package com.saxonthune.ranktheplanet.data.secure

interface SecureStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
}

expect fun createSecureStore(): SecureStore
