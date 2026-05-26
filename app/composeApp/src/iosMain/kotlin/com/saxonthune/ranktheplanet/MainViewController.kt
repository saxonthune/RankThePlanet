package com.saxonthune.ranktheplanet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.db.obtainOrCreatePassphrase
import com.saxonthune.ranktheplanet.data.op.DeviceId
import com.saxonthune.ranktheplanet.data.projection.PassthroughOverviewProjection
import com.saxonthune.ranktheplanet.data.secure.createSecureStore
import com.saxonthune.ranktheplanet.data.session.FileSessionStateStore
import com.saxonthune.ranktheplanet.data.DefaultCollectionPortIoService
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.io.IosFilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Holder assigned synchronously after ComposeUIViewController returns; safe to read
    // in the IosFilePicker lambda because file-picker ops are invoked long after construction.
    val holder = object { var vc: UIViewController? = null }

    val vc = ComposeUIViewController {
        val filePicker = remember { IosFilePicker { holder.vc!! } }
        val graph by produceState<RtpAppGraph?>(null) {
            val store = createSecureStore()
            val passphrase = obtainOrCreatePassphrase(store)
            val deviceId = DeviceId.obtainOrCreate(store)
            val driver = createDriver(passphrase)
            val db = createDatabase(driver)
            val repos = SqlRepositories(db, deviceId)
            val session = FileSessionStateStore(CoroutineScope(Dispatchers.Default + SupervisorJob()))
            val projection = PassthroughOverviewProjection(db, session)
            value = RtpAppGraph(
                repos, projection, session,
                DefaultCollectionPortIoService(repos.collections, repos.entries, repos.templates, repos.locations),
                filePicker,
            )
        }
        App(graph = graph)
    }

    holder.vc = vc
    return vc
}
