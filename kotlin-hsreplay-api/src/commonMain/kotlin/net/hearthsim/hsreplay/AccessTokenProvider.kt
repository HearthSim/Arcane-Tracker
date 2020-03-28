package net.hearthsim.hsreplay

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_ACCESS_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_REFRESH_TOKEN
import net.hearthsim.hsreplay.model.Token

class AccessTokenProvider(val preferences: Preferences, val oauthApi: HsReplayOauthApi, val analytics: Analytics) {
    val mutex = Mutex()

    private var accessToken = preferences.getString(HSREPLAY_OAUTH_ACCESS_TOKEN)
    private var refreshToken = preferences.getString(HSREPLAY_OAUTH_REFRESH_TOKEN)

    suspend fun accessToken() = mutex.withLock {
        accessToken
    }

    suspend fun refreshToken() = mutex.withLock {
        val delays = listOf(0, 0, 30, 60, 120, 240, 480)

        delays.forEach {
            delay(it * 1000.toLong())
            val response = try {
                oauthApi.refresh(refreshToken!!)
            } catch (e: Exception) {
                return@forEach
            }

            when (response.status.value / 100) {
                2 -> Unit
                4 -> {
                    // a 4xx response means the token is bad. In these cases, we should logout the user and have him log in again
                    forget()
                    analytics.logEvent("user_logged_out", mapOf("status" to response.status.value.toString()))
                    return@withLock
                }
                else -> {
                    //  other errors are usually non fatal. Try again
                    analytics.logEvent("refresh_error", mapOf("status" to response.status.value.toString()))
                    return@forEach
                }
            }
            val text = response.content.readRemaining(Long.MAX_VALUE, 16*1024).readText()
            val token = try {
                Json.nonstrict.parse(Token.serializer(), text)
            } catch (e: Exception) {
                // a parsing error is usually non fatal. Try again
                return@forEach
            }

            remember(token.access_token, token.refresh_token)
            return@withLock
        }
    }

    fun remember(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        preferences.putString(HSREPLAY_OAUTH_ACCESS_TOKEN, accessToken)
        preferences.putString(HSREPLAY_OAUTH_REFRESH_TOKEN, refreshToken)
    }

    fun forget() {
        this.accessToken = null
        this.refreshToken = null
        preferences.putString(HSREPLAY_OAUTH_ACCESS_TOKEN, null)
        preferences.putString(HSREPLAY_OAUTH_REFRESH_TOKEN, null)

    }

    fun isLoggedIn(): Boolean {
        return accessToken != null && refreshToken != null
    }
}