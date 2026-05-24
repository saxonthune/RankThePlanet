package com.saxonthune.ranktheplanet.data.op

import com.saxonthune.ranktheplanet.data.secure.SecureStore
import com.saxonthune.ranktheplanet.data.secure.SecureStoreKeys
import kotlin.random.Random

internal object DeviceId {
    suspend fun obtainOrCreate(secureStore: SecureStore): String {
        secureStore.get(SecureStoreKeys.DEVICE_ID)?.let { return it }
        val id = generateUuid()
        secureStore.put(SecureStoreKeys.DEVICE_ID, id)
        return id
    }
}

internal fun generateUuid(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    return buildString {
        bytes.forEachIndexed { i, b ->
            if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
            append((b.toInt() and 0xff).toString(16).padStart(2, '0'))
        }
    }
}
