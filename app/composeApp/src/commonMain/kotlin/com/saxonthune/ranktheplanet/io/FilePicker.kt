package com.saxonthune.ranktheplanet.io

import com.saxonthune.ranktheplanet.domain.io.PortFormat

interface FilePicker {
    /** Prompt the user to pick a file. Returns its full text content. */
    suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile>

    /** Prompt the user to save the bytes. Format dictates MIME + extension; suggestedName is a hint. */
    suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit>

    /** Hand bytes to the platform share sheet (text-content share, e.g. UIActivityViewController / ACTION_SEND). */
    suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit>
}

data class PickedFile(val name: String, val text: String, val inferredFormat: PortFormat?)

class NoOpFilePicker : FilePicker {
    override suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile> =
        Result.failure(NotImplementedError("FilePicker not wired"))

    override suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        Result.failure(NotImplementedError("FilePicker not wired"))

    override suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        Result.failure(NotImplementedError("FilePicker not wired"))
}
