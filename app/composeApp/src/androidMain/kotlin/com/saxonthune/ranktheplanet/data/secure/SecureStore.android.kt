package com.saxonthune.ranktheplanet.data.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// App.kt populates this before SecureStore is first used (Phase 3 wiring).
// Using a companion holder instead of a context constructor parameter because
// the commonMain expect fun createSecureStore() cannot accept a platform-typed arg.
object SecureStoreAndroidContext {
    lateinit var applicationContext: Context
        internal set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}

private class AndroidSecureStore(context: Context) : SecureStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rtp_secure_store",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)
    }

    override suspend fun put(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, value).apply()
    }
}

actual fun createSecureStore(): SecureStore =
    AndroidSecureStore(SecureStoreAndroidContext.applicationContext)
