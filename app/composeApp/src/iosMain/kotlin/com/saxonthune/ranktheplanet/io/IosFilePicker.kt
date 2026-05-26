@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.saxonthune.ranktheplanet.io

import com.saxonthune.ranktheplanet.domain.io.PortFormat
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

class IosFilePicker(
    private val rootViewControllerProvider: () -> UIViewController,
) : FilePicker {

    override suspend fun openForRead(formats: List<PortFormat>): Result<PickedFile> =
        suspendCancellableCoroutine { cont ->
            val types = buildList {
                UTType.typeWithFilenameExtension("kml")?.let { add(it) }
                UTType.typeWithFilenameExtension("geojson")?.let { add(it) }
                add(UTTypeJSON)
            }
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = types)
            picker.allowsMultipleSelection = false

            val delegate = OpenPickerDelegate(cont)
            picker.delegate = delegate

            dispatch_async(dispatch_get_main_queue()) {
                rootViewControllerProvider().presentViewController(picker, animated = true, completion = null)
            }

            cont.invokeOnCancellation {
                delegate // keep delegate alive until cancellation
                dispatch_async(dispatch_get_main_queue()) {
                    picker.dismissViewControllerAnimated(true, completion = null)
                }
            }
        }

    override suspend fun saveAs(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val result = writeTempFile(suggestedName, text)
            val fileUrl = result.getOrElse {
                cont.resume(Result.failure(it))
                return@suspendCancellableCoroutine
            }

            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileUrl))

            val delegate = SavePickerDelegate(cont)
            picker.delegate = delegate

            dispatch_async(dispatch_get_main_queue()) {
                rootViewControllerProvider().presentViewController(picker, animated = true, completion = null)
            }

            cont.invokeOnCancellation {
                delegate
                dispatch_async(dispatch_get_main_queue()) {
                    picker.dismissViewControllerAnimated(true, completion = null)
                }
            }
        }

    override suspend fun share(suggestedName: String, format: PortFormat, text: String): Result<Unit> =
        runCatching {
            val fileUrl = writeTempFile(suggestedName, text).getOrThrow()
            suspendCancellableCoroutine { cont ->
                dispatch_async(dispatch_get_main_queue()) {
                    val vc = UIActivityViewController(
                        activityItems = listOf(fileUrl),
                        applicationActivities = null,
                    )
                    rootViewControllerProvider().presentViewController(vc, animated = true, completion = null)
                    cont.resume(Unit)
                }
            }
        }
}

private fun writeTempFile(name: String, text: String): Result<NSURL> = runCatching {
    val path = NSTemporaryDirectory() + name
    val url = NSURL.fileURLWithPath(path)
    val nsStr = NSString.create(string = text)
    nsStr.writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    url
}

private fun inferFormat(fileName: String): PortFormat? = when {
    fileName.endsWith(".kml", ignoreCase = true) -> PortFormat.Kml
    fileName.endsWith(".geojson", ignoreCase = true) -> PortFormat.GeoJson
    fileName.endsWith(".json", ignoreCase = true) -> PortFormat.GeoJson
    else -> null
}

private class OpenPickerDelegate(
    private val cont: kotlinx.coroutines.CancellableContinuation<Result<PickedFile>>,
) : platform.darwin.NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        val urls = didPickDocumentsAtURLs as List<NSURL>
        val url = urls.firstOrNull() ?: run {
            cont.resume(Result.failure(CancellationException("no url returned")))
            return
        }

        url.startAccessingSecurityScopedResource()
        val result = runCatching {
            val data = NSData.dataWithContentsOfURL(url)
                ?: throw Exception("Cannot read file contents")
            val text = NSString.create(data, NSUTF8StringEncoding) as String?
                ?: throw Exception("Cannot decode file as UTF-8")
            val fileName = url.lastPathComponent ?: "file"
            PickedFile(fileName, text, inferFormat(fileName))
        }
        url.stopAccessingSecurityScopedResource()
        cont.resume(result)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        cont.resume(Result.failure(CancellationException("user cancelled")))
    }
}

private class SavePickerDelegate(
    private val cont: kotlinx.coroutines.CancellableContinuation<Result<Unit>>,
) : platform.darwin.NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        cont.resume(Result.success(Unit))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        cont.resume(Result.failure(CancellationException("user cancelled")))
    }
}
