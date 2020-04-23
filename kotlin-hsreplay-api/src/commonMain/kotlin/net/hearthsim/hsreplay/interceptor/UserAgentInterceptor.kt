package net.hearthsim.hsreplay.interceptor

import net.hearthsim.hsreplay.addHeader
import net.mbonnin.jolly.JollyChain
import net.mbonnin.jolly.JollyInterceptor
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.JollyResponse

class UserAgentInterceptor(val userAgent: String) : JollyInterceptor() {
    override suspend fun intercept(chain: JollyChain): JollyResponse {
        return chain.proceed(chain.request.addHeader("User-Agent", userAgent))
    }
}