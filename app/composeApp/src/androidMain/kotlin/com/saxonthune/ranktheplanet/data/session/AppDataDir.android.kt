package com.saxonthune.ranktheplanet.data.session

import com.saxonthune.ranktheplanet.data.secure.SecureStoreAndroidContext

actual fun appDataDir(): String =
    SecureStoreAndroidContext.applicationContext.filesDir.absolutePath

actual fun readFileText(path: String): String? = try {
    java.io.File(path).readText()
} catch (_: Exception) {
    null
}

actual fun writeFileText(path: String, content: String) {
    val f = java.io.File(path)
    f.parentFile?.mkdirs()
    f.writeText(content)
}
