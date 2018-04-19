package net.mbonnin.arcanetracker.hsreplay

import com.google.gson.JsonParser
import io.fabric.sdk.android.services.network.HttpRequest.HEADER_AUTHORIZATION
import net.mbonnin.arcanetracker.Utils
import okhttp3.*
import java.util.*

class OauthInterceptor : Interceptor {

    class AuthenticationException : Exception()

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(lock) {
            var request = chain.request()

            val requestBuilder = request.newBuilder()


            requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken!!}")
            request = requestBuilder.build()

            var response = chain.proceed(request)

            return response
        }
    }

    companion object {
        private const val A = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP"
        private const val B = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        private const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/"
        private const val CALLBACK_URL = "arcanetracker://callback/"

        private var accessToken: String? = null

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
                val root = JsonParser().parse(response.body()!!.string())

                synchronized(lock) {
                    accessToken = root.asJsonObject.get("access_token").asString
                    lock.notifyAll()
                }
            }
        }

        fun random(): String {
            val generator = Random()
            val randomStringBuilder = StringBuilder()
            val c = "abcdefghijklmnopqrstuvwxyz0123456789"

            for (i in 0 until 16) {
                randomStringBuilder.append(c[generator.nextInt(c.length)])
            }
            return randomStringBuilder.toString()
        }

        fun startOauth() {
            val url = HttpUrl.parse(AUTHORIZE_URL)!!
                    .newBuilder()
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("client_id", A)
                    .addQueryParameter("redirect_uri", CALLBACK_URL)
                    .addQueryParameter("scope", "fullaccess")
                    .addQueryParameter("state", random())

            Utils.openLink(url.toString())
        }
    }
}
