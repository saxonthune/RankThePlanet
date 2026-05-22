@file:OptIn(ExperimentalForeignApi::class)

package com.saxonthune.ranktheplanet.data.secure

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
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

private class IosSecureStore : SecureStore {
    @Suppress("UNCHECKED_CAST")
    override suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        memScoped {
            val query = NSMutableDictionary().apply {
                setObject(kSecClassGenericPassword, forKey = kSecClass as Any)
                setObject(SERVICE as Any, forKey = kSecAttrService as Any)
                setObject(key as Any, forKey = kSecAttrAccount as Any)
                setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as Any)
                setObject(NSNumber.numberWithBool(true), forKey = kSecReturnData as Any)
            }
            val resultRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, resultRef.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = resultRef.value as? NSData ?: return@memScoped null
            NSString.create(data, NSUTF8StringEncoding) as? String
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun put(key: String, value: String): Unit = withContext(Dispatchers.Default) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return@withContext
        val existing = get(key)
        if (existing != null) {
            val query = NSMutableDictionary().apply {
                setObject(kSecClassGenericPassword, forKey = kSecClass as Any)
                setObject(SERVICE as Any, forKey = kSecAttrService as Any)
                setObject(key as Any, forKey = kSecAttrAccount as Any)
            }
            val update = NSMutableDictionary().apply {
                setObject(data, forKey = kSecValueData as Any)
            }
            SecItemUpdate(query as CFDictionaryRef, update as CFDictionaryRef)
        } else {
            val item = NSMutableDictionary().apply {
                setObject(kSecClassGenericPassword, forKey = kSecClass as Any)
                setObject(SERVICE as Any, forKey = kSecAttrService as Any)
                setObject(key as Any, forKey = kSecAttrAccount as Any)
                setObject(data, forKey = kSecValueData as Any)
            }
            SecItemAdd(item as CFDictionaryRef, null)
        }
    }
}

actual fun createSecureStore(): SecureStore = IosSecureStore()
