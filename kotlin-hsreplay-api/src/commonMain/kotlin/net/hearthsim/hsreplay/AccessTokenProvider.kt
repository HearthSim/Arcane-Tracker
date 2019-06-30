package net.hearthsim.hsreplay

import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_ACCESS_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_REFRESH_TOKEN
import net.hearthsim.hsreplay.model.Token

class AccessTokenProvider(val preferences: Preferences, val oauthApi: HsReplayOauthApi) {
    val mutex = Mutex()

    private var accessToken = preferences.getString(HSREPLAY_OAUTH_ACCESS_TOKEN)
    private var refreshToken = preferences.getString(HSREPLAY_OAUTH_REFRESH_TOKEN)

    suspend fun accessToken() = mutex.withLock {
        accessToken
    }

    suspend fun refreshToken() = mutex.withLock {
        val response = oauthApi.refresh(refreshToken!!)

        val text= response.content.readRemaining().readText()
        val token = Json.nonstrict.parse(Token.serializer(), text)

        remember(token.access_token, token.refresh_token)
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