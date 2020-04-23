package net.hearthsim.hsreplay

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import net.hearthsim.analytics.Analytics
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_ACCESS_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_REFRESH_TOKEN
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.Token
import okio.buffer
import okio.internal.commonToUtf8String

class AccessTokenProvider(val preferences: Preferences,
                          val analytics: Analytics,
                          userAgent: String,
                          oauthParams: OauthParams
) {
    private val mutex = Mutex()

    private val oauthApi: HsReplayOauthApi = HsReplayOauthApi(
            UserAgentInterceptor(userAgent),
            oauthParams
    )

    private var accessToken = preferences.getString(HSREPLAY_OAUTH_ACCESS_TOKEN)
    private var refreshToken = preferences.getString(HSREPLAY_OAUTH_REFRESH_TOKEN)

    suspend fun accessToken() = mutex.withLock {
        accessToken
    }

    suspend fun refreshToken() = mutex.withLock {
        val delays = listOf(0, 0, 30, 60, 120, 240, 480)

        delays.forEach {
            delay(it * 1000.toLong())
            val response = oauthApi.refresh(refreshToken!!)

            if (response !is HSReplayResult.Success) {
                return@forEach
            }

            val code = response.value.statusCode
            when (response.value.statusCode / 100) {
                2 -> Unit
                4 -> {
                    // a 4xx response means the token is bad. In these cases, we should logout the user and have him log in again
                    logout()
                    analytics.logEvent("user_logged_out", mapOf("status" to code.toString()))
                    return@withLock
                }
                else -> {
                    //  other errors are usually non fatal. Try again
                    analytics.logEvent("refresh_error", mapOf("status" to code.toString()))
                    return@forEach
                }
            }
            val text = response.value.body?.commonToUtf8String()
            val token = try {
                Json.nonstrict.parse(Token.serializer(), text ?: "")
            } catch (e: Exception) {
                // a parsing error is usually non fatal. Try again
                return@forEach
            }

            remember(token.access_token, token.refresh_token)
            return@withLock
        }
    }

    private fun remember(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        preferences.putString(HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken)
        preferences.putString(HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken)
    }

    fun logout() {
        this.accessToken = null
        this.refreshToken = null
        preferences.putString(HSREPLAY_OAUTH_ACCESS_TOKEN, null)
        preferences.putString(HSREPLAY_OAUTH_REFRESH_TOKEN, null)

    }

    fun isLoggedIn(): Boolean {
        return accessToken != null && refreshToken != null
    }

    fun login(accessToken: String, refreshToken: String) {
        remember(accessToken, refreshToken)
    }

    suspend fun login(code: String): HSReplayResult<Unit> {
        val tokenResult = oauthApi.login(code)

        if (tokenResult !is HSReplayResult.Success) {
            return HSReplayResult.Error((tokenResult as HSReplayResult.Error).exception)
        } else {
            val token = tokenResult.value
            remember(token.access_token, token.refresh_token)
            return HSReplayResult.Success(Unit)
        }
    }
}