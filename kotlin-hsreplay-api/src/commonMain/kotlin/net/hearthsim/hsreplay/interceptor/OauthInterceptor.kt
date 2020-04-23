package net.hearthsim.hsreplay.interceptor

import net.hearthsim.hsreplay.AccessTokenProvider
import net.hearthsim.hsreplay.addHeader
import net.mbonnin.jolly.JollyChain
import net.mbonnin.jolly.JollyInterceptor
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.JollyResponse

class OauthInterceptor(val accessTokenProvider: AccessTokenProvider) : JollyInterceptor() {

    private fun JollyRequest.withAuthorizationHeader(accessToken: String?): JollyRequest {
        return addHeader("Authorization", "Bearer $accessToken")
    }

    override suspend fun intercept(chain: JollyChain): JollyResponse {
        val accessToken = accessTokenProvider.accessToken()
        println("intercept: $accessToken")
        var response: JollyResponse? = null
        if (accessToken != null) {
            response = chain.proceed(chain.request.withAuthorizationHeader(accessTokenProvider.accessToken()))
        }

        if (response == null || response.statusCode == 401 || response.statusCode == 403) {
            // Right now (April 2020), sending a wrong access token results in a 403 error
            println("refresh: $accessToken")
            // there's a race here and we might end up refreshing the token more than strictly necessary
            accessTokenProvider.refreshToken()
            response = chain.proceed(chain.request.withAuthorizationHeader(accessTokenProvider.accessToken()))
        }
        return response
    }
}
