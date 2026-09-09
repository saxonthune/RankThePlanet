package com.saxonthune.ranktheplanet.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.saxonthune.ranktheplanet.data.secure.SecureStoreAndroidContext
import com.saxonthune.ranktheplanet.db.AppDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

actual fun createDriver(passphrase: String): SqlDriver {
    val factory = SupportOpenHelperFactory(passphrase.toByteArray())
    return AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = SecureStoreAndroidContext.applicationContext,
        name = "rtp.db",
        factory = factory,
    )
}
