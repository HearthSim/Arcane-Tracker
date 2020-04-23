package net.mbonnin.jolly

import kotlinx.cinterop.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.buffer
import okio.toByteString
import platform.Foundation.*
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.native.concurrent.freeze
import platform.Foundation.setHTTPMethod
import kotlin.coroutines.resume

actual class JollyEngine {
    private fun ByteArray.toNSData(): NSData = NSMutableData().apply {
        if (isEmpty()) return@apply
        this@toNSData.usePinned {
            appendBytes(it.addressOf(0), size.convert())
        }
    }

    actual suspend fun execute(jollyRequest: JollyRequest): JollyResponse {
        val session = NSURLSession.sessionWithConfiguration(NSURLSessionConfiguration.defaultSessionConfiguration())

        assert(NSThread.isMainThread())

        val request = NSMutableURLRequest.requestWithURL(NSURL(string = jollyRequest.url)).apply {
            setHTTPMethod(jollyRequest.method.name)
            setCachePolicy(NSURLRequestReloadIgnoringCacheData)
            jollyRequest.headers.forEach {
                setValue(it.value, forHTTPHeaderField = it.key)
            }
            jollyRequest.body?.let {
                setHTTPBody(it.toNSData())
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val continuationRef = StableRef.create(continuation).asCPointer()

            val delegate = { httpData: NSData?, httpResponse: NSURLResponse?, error: NSError? ->
                initRuntimeIfNeeded()
                val response = JollyResponse(
                        statusCode = (httpResponse as NSHTTPURLResponse).statusCode.toInt(),
                        body = httpData?.bytes?.readBytes(httpData.length.toInt()),
                        error = error?.description?.let {Exception(it)}
                )

                dispatch_async_f(
                        queue = dispatch_get_main_queue(),
                        context = StableRef.create((continuationRef to response).freeze()).asCPointer(),
                        work = staticCFunction { it ->
                            val continuationRefAndResponseRef = it!!.asStableRef<Pair<COpaquePointer,JollyResponse>>()
                            val continuationRefAndResponse = continuationRefAndResponseRef.get()
                            continuationRefAndResponseRef.dispose()

                            val continuationRef = continuationRefAndResponse.first.asStableRef<CancellableContinuation<JollyResponse>>()
                            continuationRef.get().resume(continuationRefAndResponse.second)
                            continuationRef.dispose()
                        }
                )
            }

            session.dataTaskWithRequest(
                    request.freeze(),
                    delegate.freeze()
            ).resume()
        }
    }
}