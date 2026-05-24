package com.saxonthune.ranktheplanet.data.session

expect fun appDataDir(): String
expect fun readFileText(path: String): String?
expect fun writeFileText(path: String, content: String)
