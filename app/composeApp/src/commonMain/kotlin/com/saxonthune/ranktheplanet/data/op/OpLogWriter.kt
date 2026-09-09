package com.saxonthune.ranktheplanet.data.op

import com.saxonthune.ranktheplanet.db.AppDatabase

internal class OpLogWriter(
    private val database: AppDatabase,
    private val deviceId: String,
) {
    fun append(op: Op) {
        database.opLogQueries.append(
            op_id = generateUuid(),
            ts = nowIso(),
            device_id = deviceId,
            kind = op.kind,
            payload = op.toPayloadJson(),
        )
    }
}
