package net.mbonnin.arcanetracker.hsreplay

import com.squareup.moshi.Moshi
import io.fabric.sdk.android.services.network.HttpRequest.HEADER_AUTHORIZATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils
import okhttp3.*
import java.io.IOException

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
                val result = refreshToken()
                if (result.e != null) {
                    Utils.reportNonFatal(result.e)
                    throw result.e
                }

                requestBuilder.header(HEADER_AUTHORIZATION, "Bearer ${accessToken}")
                response = chain.proceed(requestBuilder.build())
            }

            return response
        }
    }


    class RefreshResult(val e: IOException?, val recoverable: Boolean = true)

    companion object {
        const val A = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP"
        const val B = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client"
        const val CALLBACK_URL = "arcanetracker://callback/"

        private var accessToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, null)
        var refreshToken: String? = Settings.getString(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, null)

        private val lock = Object()


        private fun storeToken(response: Response): Result<Unit> {
            val tokenResponse = response.body()?.string()
            if (tokenResponse == null) {
                val e = Exception("Body Error")
                return Result.failure(e)
            }

            try {
                val map = Moshi.Builder().build().adapter<Map<String, String>>(Map::class.java).fromJson(tokenResponse)!!
                accessToken = map.get("access_token")
                refreshToken = map.get("refresh_token")
            } catch (e: Exception) {
                return Result.failure(e)
            }

            Settings.set(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken!!)
            Settings.set(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken!!)

            return Result.success(Unit)
        }

        /**
         * Configures the interceptor with a code got from a Oauth client flow. This will
         * exchange the code and remember it for future usage.
         * This will block, do not call from main thread
         */
        suspend fun login(code: String): Result<Unit> = withContext(Dispatchers.IO) {
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
                return@withContext Result.failure<Unit>(e)
            }

            if (!response.isSuccessful) {
                val e = Exception("HTTP error ${response.code()}")
                return@withContext Result.failure<Unit>(e)
            }

            synchronized(lock) {
                val r = storeToken(response)
                if (r.isFailure) {
                    return@withContext r
                }
                lock.notifyAll()
            }

            return@withContext Result.success(Unit)
        }

        fun logout() {
            accessToken = null
            refreshToken = null

            Settings.remove(Settings.HSREPLAY_OAUTH_ACCESS_TOKEN)
            Settings.remove(Settings.HSREPLAY_OAUTH_REFRESH_TOKEN)

        }

        fun refreshToken(): RefreshResult {
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

            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                return RefreshResult(e, recoverable = true)
            }

            if (!response.isSuccessful) {
                return RefreshResult(IOException("HTTP token refresh error ${response.code()}"), recoverable = false)
            }

            if (storeToken(response).isFailure) {
                return RefreshResult(IOException("Cannot store token"), recoverable = false)
            }

            return RefreshResult(null)
        }
    }
}
