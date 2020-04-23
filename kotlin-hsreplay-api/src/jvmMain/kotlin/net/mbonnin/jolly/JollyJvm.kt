package net.mbonnin.jolly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.buffer


actual class JollyEngine {
    actual suspend fun execute(jollyRequest: JollyRequest): JollyResponse {
        val contentType = jollyRequest.headers.get("Content-Type")
                ?: "application/octet-stream"

        val body = if (jollyRequest.body != null) {
            RequestBody.create(MediaType.parse(contentType), jollyRequest.body)
        } else {
            null
        }
        return Request.Builder().apply {
            method(jollyRequest.method.name, body)
            url(jollyRequest.url)
            jollyRequest.headers.entries.forEach {
                addHeader(it.key, it.value)
            }
        }
                .build()
                .let {
                    withContext(Dispatchers.IO) {
                        try {
                            val response = OkHttpClient.Builder().apply {
                                val logging = HttpLoggingInterceptor()
                                logging.level = HttpLoggingInterceptor.Level.BODY
                                addInterceptor(logging)
                            }
                                    .build()
                                    .newCall(it).execute()
                            JollyResponse(
                                    statusCode = response.code(),
                                    body = response.body()?.bytes(),
                                    error = null
                            )
                        } catch (e: Exception) {
                            JollyResponse(
                                    statusCode = 0,
                                    body = null,
                                    error = e
                            )
                        }
                    }
                }
    }
}