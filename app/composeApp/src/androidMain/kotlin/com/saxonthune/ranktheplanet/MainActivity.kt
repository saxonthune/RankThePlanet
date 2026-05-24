package com.saxonthune.ranktheplanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.saxonthune.ranktheplanet.data.db.createDatabase
import com.saxonthune.ranktheplanet.data.db.createDriver
import com.saxonthune.ranktheplanet.data.db.obtainOrCreatePassphrase
import com.saxonthune.ranktheplanet.data.op.DeviceId
import com.saxonthune.ranktheplanet.data.projection.PassthroughOverviewProjection
import com.saxonthune.ranktheplanet.data.secure.SecureStoreAndroidContext
import com.saxonthune.ranktheplanet.data.secure.createSecureStore
import com.saxonthune.ranktheplanet.data.session.FileSessionStateStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureStoreAndroidContext.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val graph by produceState<RtpAppGraph?>(null) {
                val store = createSecureStore()
                val passphrase = obtainOrCreatePassphrase(store)
                val deviceId = DeviceId.obtainOrCreate(store)
                val driver = createDriver(passphrase)
                val db = createDatabase(driver)
                val repos = SqlRepositories(db, deviceId)
                val session = FileSessionStateStore(CoroutineScope(Dispatchers.IO + SupervisorJob()))
                val projection = PassthroughOverviewProjection(db, session)
                value = RtpAppGraph(repos, projection, session)
            }
            App(graph = graph)
        }
    }
}
