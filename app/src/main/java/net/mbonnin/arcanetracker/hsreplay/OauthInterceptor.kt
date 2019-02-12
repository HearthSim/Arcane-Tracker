package net.mbonnin.arcanetracker.hsreplay

import com.google.gson.JsonParser
import io.fabric.sdk.android.services.network.HttpRequest.HEADER_AUTHORIZATION
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils
import okhttp3.*
import timber.log.Timber

class OauthInterceptor : Interceptor {

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(lock) {
            var request = chain.request()

            val requestBuilder = request.newBuilder()

            requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken}")
            request = requestBuilder.build()

            var response = chain.proceed(request)
            if (!response.isSuccessful && accessToken != null && refreshToken != null) {
                refreshToken()

                requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken}")
                response = chain.proceed(requestBuilder.build())
            }

            return response
        }
    }

    companion object {
        const val A = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP"
        const val B = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client"
        const val CALLBACK_URL = "arcanetracker://callback/"

        private var accessToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, null)
        var refreshToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, null)

        private val lock = java.lang.Object()

        fun exchangeCode(code: String) {
            val client = OkHttpClient()

            val httpUrl = HttpUrl.parse("https://hsreplay.net/oauth2/token/")!!
                    .newBuilder()
                    .addQueryParameter("code", code)
                    .addQueryParameter("client_id", A)
                    .addQueryParameter("client_secret", B)
                    .addQueryParameter("grant_type", "authorization_code")
                    .addQueryParameter("redirect_uri", CALLBACK_URL)
                    .build()

            val body = FormBody.Builder()
                    .build()

            val request = Request.Builder()
                    .post(body)
                    .url(httpUrl)
                    .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP error ${response.code()}")
            } else {

                try {
                    val json = response.body()!!.string()
                    val root = JsonParser().parse(json)

                    synchronized(lock) {
                        accessToken = root.asJsonObject.get("access_token").asString
                        refreshToken = root.asJsonObject.get("refresh_token").asString

                        Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken!!)
                        Settings.set(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken!!)
                        lock.notifyAll()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        fun unlink() {
            accessToken = null
            refreshToken = null

            Settings.remove(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN)
            Settings.remove(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN)

        }

        fun refreshToken() {
            val client = OkHttpClient()

            val httpUrl = HttpUrl.parse("https://hsreplay.net/oauth2/token/")!!
                    .newBuilder()
                    .build()

            val body = FormBody.Builder()
                    .add("client_id", A)
                    .add("client_secret", B)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()

            val request = Request.Builder()
                    .post(body)
                    .url(httpUrl)
                    .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP error ${response.code()}")
            } else {

                try {
                    val json = response.body()!!.string()
                    val root = JsonParser().parse(json)

                    accessToken = root.asJsonObject.get("access_token").asString
                    refreshToken = root.asJsonObject.get("refresh_token").asString

                    Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken!!)
                    Settings.set(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken!!)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }
}
