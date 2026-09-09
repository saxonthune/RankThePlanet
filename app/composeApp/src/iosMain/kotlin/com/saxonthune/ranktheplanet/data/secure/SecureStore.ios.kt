@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.saxonthune.ranktheplanet.data.secure

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFAutorelease
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "com.saxonthune.ranktheplanet"

private fun query(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
    val dict = CFDictionaryCreateMutable(null, pairs.size.convert(), null, null)
    pairs.forEach { (k, v) -> CFDictionaryAddValue(dict, k, v) }
    CFAutorelease(dict)
    return dict
}

private class IosSecureStore : SecureStore {
    override suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        val serviceRef = CFBridgingRetain(SERVICE)
        val accountRef = CFBridgingRetain(key)
        try {
            memScoped {
                val q = query(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to serviceRef,
                    kSecAttrAccount to accountRef,
                    kSecMatchLimit to kSecMatchLimitOne,
                    kSecReturnData to kCFBooleanTrue,
                )
                val resultRef = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(q, resultRef.ptr)
                if (status != errSecSuccess) return@memScoped null
                val data = CFBridgingRelease(resultRef.value) as? NSData ?: return@memScoped null
                NSString.create(data, NSUTF8StringEncoding) as String?
            }
        } finally {
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
        }
    }

    override suspend fun put(key: String, value: String): Unit = withContext(Dispatchers.Default) {
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return@withContext
        val serviceRef = CFBridgingRetain(SERVICE)
        val accountRef = CFBridgingRetain(key)
        val dataRef = CFBridgingRetain(data)
        try {
            val existing = get(key)
            if (existing != null) {
                val q = query(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to serviceRef,
                    kSecAttrAccount to accountRef,
                )
                val update = query(
                    kSecValueData to dataRef,
                )
                SecItemUpdate(q, update)
            } else {
                val item = query(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to serviceRef,
                    kSecAttrAccount to accountRef,
                    kSecValueData to dataRef,
                )
                SecItemAdd(item, null)
            }
        } finally {
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
            CFBridgingRelease(dataRef)
        }
    }
}

actual fun createSecureStore(): SecureStore = IosSecureStore()
