package com.saxonthune.ranktheplanet.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.saxonthune.ranktheplanet.db.AppDatabase

actual fun createDriver(passphrase: String): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
