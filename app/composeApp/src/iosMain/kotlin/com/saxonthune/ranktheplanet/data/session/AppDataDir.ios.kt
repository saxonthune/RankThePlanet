@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.saxonthune.ranktheplanet.data.session

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@Suppress("UNCHECKED_CAST")
actual fun appDataDir(): String =
    (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .first() as String)

actual fun readFileText(path: String): String? = try {
    NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
} catch (_: Exception) {
    null
}

actual fun writeFileText(path: String, content: String) {
    val dir = path.substringBeforeLast("/")
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
}
