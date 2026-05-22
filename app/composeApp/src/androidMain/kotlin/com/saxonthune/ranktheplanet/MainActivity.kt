package com.saxonthune.ranktheplanet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.saxonthune.ranktheplanet.data.secure.SecureStoreAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureStoreAndroidContext.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
