package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.Token
import net.mbonnin.jolly.*

class HsReplayOauthApi(val userAgentInterceptor: UserAgentInterceptor,
                       val oauthParams: OauthParams) {
    private val client = JollyClient().apply {
        addInterceptor(userAgentInterceptor)
    }

    companion object {
        const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client"
    }

    private fun String.urlencode(): String {
        return UrlEncoder.encode(this)
    }
    suspend fun login(code: String): HSReplayResult<Token> {
        val body = "code=${code.urlencode()}" +
                "&client_id=${oauthParams.clientId.urlencode()}" +
                "&client_secret=${oauthParams.clientSecret.urlencode()}" +
                "&grant_type=authorization_code" +
                "&redirect_uri=${oauthParams.redirectUri.urlencode()}" // This is strictly not required by Oauth but the server complains without

        return JollyRequest {
            method(Method.POST)
            url("https://hsreplay.net/oauth2/token/")
            body("application/x-www-form-urlencoded", body)
        }.execute(client, Token.serializer())
    }

    //code=7mmuMTLTDNYo1k6suOLwsPceiGDmKP&client_id=pk_test_bEXSxNIGHk6E3q0Pu8McJpwJ&client_secret=sk_test_20180319rRyl1qhSfbZ4Vf0kJXgMfsVi&grant_type=authorization_code&redirect_uri=https%3A%2F%2Flocalhost%3A9000%2F
    //code=b6LLYZlRWfCRPY11PVN0iKvWmZ8QVA&client_id=pk_test_bEXSxNIGHk6E3q0Pu8McJpwJ&client_secret=sk_test_20180319rRyl1qhSfbZ4Vf0kJXgMfsVi&grant_type=authorization_code&redirect_uri=https%3A%2F%2Flocalhost%3A9000%2F

    suspend fun refresh(refreshToken: String): HSReplayResult<JollyResponse>  {
        val body = "client_id=${oauthParams.clientId.urlencode()}" +
                "&client_secret=${oauthParams.clientSecret.urlencode()}" +
                "&grant_type=refresh_token" +
                "&refresh_token=${refreshToken.urlencode()}"

        return JollyRequest {
            method(Method.POST)
            url("https://hsreplay.net/oauth2/token/")
            body("application/x-www-form-urlencoded", body)
        }.execute(client)
    }
}