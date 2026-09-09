package com.saxonthune.ranktheplanet.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.saxonthune.ranktheplanet.db.AppDatabase

// TODO: iOS driver uses the system SQLite unencrypted. SQLCipher on iOS requires a custom
// SQLite open hook on NativeSqliteDriver. See https://cashapp.github.io/sqldelight/2.0.2/native_sqlite/
// for the integration path. Address in a follow-up once phase 1 substrate is confirmed.
actual fun createDriver(passphrase: String): SqlDriver =
    NativeSqliteDriver(AppDatabase.Schema, "rtp.db")
