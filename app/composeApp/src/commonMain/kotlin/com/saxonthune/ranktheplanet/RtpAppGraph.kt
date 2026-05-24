package com.saxonthune.ranktheplanet

import com.saxonthune.ranktheplanet.data.projection.OverviewProjection
import com.saxonthune.ranktheplanet.data.session.SessionStateStore
import com.saxonthune.ranktheplanet.data.sql.SqlRepositories

data class RtpAppGraph(
    val repos: SqlRepositories,
    val projection: OverviewProjection,
    val session: SessionStateStore,
)
