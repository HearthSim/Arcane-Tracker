package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.interceptor.GzipInterceptor
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.mbonnin.jolly.JollyClient
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.JollyResponse
import net.mbonnin.jolly.Method

class HsReplayS3GameApi(val userAgentInterceptor: UserAgentInterceptor) {
    private val client = JollyClient().apply {
        addInterceptor(userAgentInterceptor)
        addInterceptor(GzipInterceptor())
    }


    suspend fun put(putUrl: String, gameString: ByteArray): HSReplayResult<JollyResponse> {
        return JollyRequest {
            method(Method.PUT)
            body("text/plain", gameString)
            url(putUrl)

        }.execute(client)
    }
}