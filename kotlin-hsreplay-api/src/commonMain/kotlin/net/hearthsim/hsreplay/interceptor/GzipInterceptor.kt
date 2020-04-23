package net.hearthsim.hsreplay.interceptor

import net.mbonnin.jolly.GzipEncoder
import net.mbonnin.jolly.JollyChain
import net.mbonnin.jolly.JollyInterceptor
import net.mbonnin.jolly.JollyResponse
import okio.Buffer
import okio.buffer

class GzipInterceptor : JollyInterceptor() {
    override suspend fun intercept(chain: JollyChain): JollyResponse {
        val newHeaders = chain.request.headers.toMutableMap().apply {
            put("Content-Encoding", "gzip")
        }

        val content = GzipEncoder.encode(chain.request.body!!)

        val newRequest  = chain.request.copy(
                headers = newHeaders,
                body = content
        )
        return chain.proceed(newRequest)
    }

}