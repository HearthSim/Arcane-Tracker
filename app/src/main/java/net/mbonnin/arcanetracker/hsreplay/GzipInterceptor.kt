package net.mbonnin.arcanetracker.hsreplay

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.GzipSink
import okio.Okio
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream


/** This interceptor compresses the HTTP request body. Many webservers can't handle this!  */
internal class GzipInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
            return chain.proceed(originalRequest)
        }

        val outputStream = ByteArrayOutputStream()
        val sink = Okio.buffer(Okio.sink(outputStream))
        val gzipSink = Okio.buffer(GzipSink(sink))
        originalRequest.body()!!.writeTo(gzipSink)
        gzipSink.flush()
        gzipSink.close()

        val newBody = RequestBody.create(MediaType.parse("text/plain"),
                outputStream.toByteArray())

        val compressedRequest = originalRequest.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(originalRequest.method(), newBody)
                .build()
        return chain.proceed(compressedRequest)
    }
}