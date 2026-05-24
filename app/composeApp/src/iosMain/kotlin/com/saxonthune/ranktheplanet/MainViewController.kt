package com.saxonthune.ranktheplanet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.window.ComposeUIViewController
import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.db.obtainOrCreatePassphrase
import com.saxonthune.ranktheplanet.data.op.DeviceId
import com.saxonthune.ranktheplanet.data.secure.createSecureStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories

fun MainViewController() = ComposeUIViewController {
    val repos by produceState<SqlRepositories?>(null) {
        val store = createSecureStore()
        val passphrase = obtainOrCreatePassphrase(store)
        val deviceId = DeviceId.obtainOrCreate(store)
        val driver = createDriver(passphrase)
        val db = createDatabase(driver)
        value = SqlRepositories(db, deviceId)
    }
    App(repos = repos)
}
