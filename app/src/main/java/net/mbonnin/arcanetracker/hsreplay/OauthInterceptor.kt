package net.mbonnin.arcanetracker.hsreplay

import io.fabric.sdk.android.services.network.HttpRequest.HEADER_AUTHORIZATION
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.*

class OauthInterceptor : Interceptor {
    var refreshToken = Settings.getString(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, null)
    var accessToken = Settings.getString(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, null)
    private val lock = java.lang.Object()

    fun random(): String {
        val generator = Random()
        val randomStringBuilder = StringBuilder()
        val c = "abcdefghijklmnopqrstuvwxyz0123456789"

        for (i in 0 until 16) {
            randomStringBuilder.append(c[generator.nextInt(c.length)])
        }
        return randomStringBuilder.toString()
    }

    class AuthenticationException : Exception()

    fun setTokens(accessToken: String, refreshToken: String) {
        synchronized(lock) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken

            Settings.set(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken)
            Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken)

            lock.notifyAll()
        }
    }

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(lock) {
            var request = chain.request()

            val requestBuilder = request.newBuilder()

            if (accessToken == null) {
                getAccessTokenOrThrow()
            }

            requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken!!}")
            request = requestBuilder.build()

            var response = chain.proceed(request)

            if (response.code() == 401) {
                getAccessTokenOrThrow()

                requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken!!}")

                request = requestBuilder.build()
                response = chain.proceed(request) //repeat request with new token
            }

            return response
        }
    }

    private fun getAccessTokenOrThrow() {
        if (refreshToken == null) {
            startOauth()

            val startMillis = System.currentTimeMillis()
            val timeoutMillis = 2 * 60 * 1000
            while (refreshToken == null) {
                lock.wait(1000)
                if (System.currentTimeMillis() - startMillis < timeoutMillis) {
                    Timber.e("Timeout waiting for token")
                    throw AuthenticationException()
                }
            }
        } else {
            accessToken = getNewAccessToken(refreshToken!!)
            if (accessToken == null) {
                throw AuthenticationException()
            } else {
                Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken!!)
            }
        }
    }

    private fun startOauth() {
        val r = random()

        val url = "https://hsreplay.net/oauth2/authorize/?" +
                "response_type=code" +
                "&client_id=pk_live_iKPWQuznmNf2BbBCxZa1VzmP" +
                "&redirect_uri=arcanetracker%3A%2F%2Fcallback%2F" +
                "&scope=fullaccess" +
                "&state=" + r

        Utils.openLink(url)
    }

    private fun getNewAccessToken(refreshToken: String): String? {
        return null
    }

}
