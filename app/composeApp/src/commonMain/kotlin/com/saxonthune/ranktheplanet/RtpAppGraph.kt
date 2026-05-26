package com.saxonthune.ranktheplanet

import com.saxonthune.ranktheplanet.data.CollectionPortIoService
import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories
import com.saxonthune.ranktheplanet.io.FilePicker

data class RtpAppGraph(
    val repos: SqlRepositories,
    val projection: OverviewProjection,
    val session: SessionStateStore,
    val portIo: CollectionPortIoService,
    val filePicker: FilePicker,
)
