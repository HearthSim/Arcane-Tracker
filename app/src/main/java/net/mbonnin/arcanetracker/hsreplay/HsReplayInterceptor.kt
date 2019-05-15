package net.mbonnin.arcanetracker.hsreplay

import com.squareup.moshi.Moshi
import io.fabric.sdk.android.services.network.HttpRequest.HEADER_AUTHORIZATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils
import okhttp3.*
import timber.log.Timber

class HsReplayInterceptor : Interceptor {

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

    enum class Result {
        SUCCESS,
        ERROR_BODY,
        ERROR_JSON,
        ERROR_NETWORK,
        ERROR_HTTP
    }

    companion object {
        const val A = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP"
        const val B = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client"
        const val CALLBACK_URL = "arcanetracker://callback/"

        private var accessToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, null)
        var refreshToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, null)

        private val lock = Object()


        private fun storeToken(response: Response): Result {
            val tokenResponse = response.body()?.string()
            if (tokenResponse == null) {
                Utils.reportNonFatal(Exception("Body Error"))
                return Result.ERROR_BODY
            }

            try {
                val map = Moshi.Builder().build().adapter<Map<String, String>>(Map::class.java).fromJson(tokenResponse)!!
                accessToken = map.get("access_token")
                refreshToken = map.get("refresh_token")
            } catch (e: Exception) {
                Utils.reportNonFatal(e)
                return Result.ERROR_JSON
            }

            Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken!!)
            Settings.set(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken!!)

            return Result.SUCCESS
        }

        /**
         * Configures the interceptor with a code got from a Oauth client flow. This will
         * exchange the code and remember it for future usage.
         * This will block, do not call from main thread
         */
        suspend fun configure(code: String): Result = withContext(Dispatchers.IO) {
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

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                Utils.reportNonFatal(e)
                return@withContext Result.ERROR_NETWORK
            }

            if (!response.isSuccessful) {
                Utils.reportNonFatal(Exception("HTTP error ${response.code()}"))
                return@withContext Result.ERROR_HTTP
            }

            synchronized(lock) {
                val r = storeToken(response)
                if (r != Result.SUCCESS) {
                    return@withContext r
                }
                lock.notifyAll()
            }

            return@withContext Result.SUCCESS
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
                    .add("refresh_token", refreshToken!!)
                    .build()

            val request = Request.Builder()
                    .post(body)
                    .url(httpUrl)
                    .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP token refresh error ${response.code()}")
            }

            if (storeToken(response) != Result.SUCCESS) {
                throw Exception("Cannot store token")
            }
        }
    }
}
