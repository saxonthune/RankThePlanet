package com.saxonthune.ranktheplanet.io

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.saxonthune.ranktheplanet.domain.io.PortFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

class AndroidFilePicker(
    private val activityProvider: () -> ComponentActivity,
) : FilePicker {

    override suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile> =
        suspendCancellableCoroutine { cont ->
            val activity = activityProvider()
            val key = "filepicker-open-${UUID.randomUUID()}"
            var launcher: ActivityResultLauncher<Array<String>>? = null
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.OpenDocument(),
            ) { uri: Uri? ->
                val reg = launcher
                launcher = null
                reg?.unregister()
                if (uri == null) {
                    cont.resume(Result.failure(CancellationException("user cancelled")))
                    return@register
                }
                val result = runCatching {
                    val fileName = getDisplayName(activity, uri)
                    val stream = activity.contentResolver.openInputStream(uri)
                        ?: throw IOException("Cannot open stream for $uri")
                    val text = stream.bufferedReader().use { it.readText() }
                    PickedFile(fileName, text, inferFormat(fileName))
                }
                cont.resume(result)
            }
            launcher!!.launch(
                arrayOf(
                    PortFormat.Kml.mimeType,
                    PortFormat.GeoJson.mimeType,
                    "application/xml",
                    "application/json",
                    "*/*",
                )
            )
            cont.invokeOnCancellation {
                val reg = launcher
                launcher = null
                reg?.unregister()
            }
        }

    override suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val activity = activityProvider()
            val key = "filepicker-save-${UUID.randomUUID()}"
            var launcher: ActivityResultLauncher<String>? = null
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.CreateDocument(format.mimeType),
            ) { uri: Uri? ->
                val reg = launcher
                launcher = null
                reg?.unregister()
                if (uri == null) {
                    cont.resume(Result.failure(CancellationException("user cancelled")))
                    return@register
                }
                val result = runCatching {
                    activity.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("Cannot open output stream for $uri")
                }
                cont.resume(result)
            }
            launcher!!.launch(suggestedName)
            cont.invokeOnCancellation {
                val reg = launcher
                launcher = null
                reg?.unregister()
            }
        }

    override suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        runCatching {
            val activity = activityProvider()
            val exportDir = File(activity.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, suggestedName)
            file.writeText(text, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Export $suggestedName"))
        }
}

private fun getDisplayName(activity: ComponentActivity, uri: Uri): String =
    activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    } ?: uri.lastPathSegment ?: "file"

private fun inferFormat(fileName: String): PortFormat? = when {
    fileName.endsWith(".kml", ignoreCase = true) -> PortFormat.Kml
    fileName.endsWith(".geojson", ignoreCase = true) -> PortFormat.GeoJson
    fileName.endsWith(".json", ignoreCase = true) -> PortFormat.GeoJson
    else -> null
}
