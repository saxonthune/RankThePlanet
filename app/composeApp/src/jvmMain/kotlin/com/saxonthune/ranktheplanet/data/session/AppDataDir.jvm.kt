package com.saxonthune.ranktheplanet.data.session

actual fun appDataDir(): String =
    "${System.getProperty("user.home")}/.ranktheplanet"

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
