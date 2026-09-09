package com.saxonthune.ranktheplanet.data.db

import app.cash.sqldelight.db.SqlDriver
import com.saxonthune.ranktheplanet.db.AppDatabase

expect fun createDriver(passphrase: String): SqlDriver

fun createDatabase(driver: SqlDriver): AppDatabase = AppDatabase(driver)
