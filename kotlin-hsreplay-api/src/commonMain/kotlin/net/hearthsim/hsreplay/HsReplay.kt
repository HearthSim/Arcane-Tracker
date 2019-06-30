package net.hearthsim.hsreplay

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_BATTLETAG
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_LEGACY_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_USERNAME
import net.hearthsim.hsreplay.model.legacy.UploadToken
import net.hearthsim.hsreplay.model.new.ClaimInput

class HsReplay(val preferences: Preferences) {
    private val oauthApi = HsReplayOauthApi()
    private val legacyApi = HsReplayLegacyApi()
    private val accessTokenProvider = AccessTokenProvider(preferences,oauthApi)
    private val newApi = HsReplayNewApi(accessTokenProvider)


    suspend fun login(code: String): Result<Unit> = coroutineScope {
        val uploadTokenDeferred = async {
            try {
                legacyApi.createUploadToken()
            } catch (e: Exception) {
                return@async Result.failure<Unit>(e)
            }
        }

        val token = try {
            oauthApi.login(code)
        } catch (e: Exception) {
            return@coroutineScope Result.failure<Unit>(e)
        }

        val uploadToken = uploadTokenDeferred.await()
        if (uploadToken !is UploadToken) {
            return@coroutineScope Result.failure<Unit>(uploadToken as Exception)
        }

        accessTokenProvider.remember(token.access_token, token.refresh_token)

        val claimResult = try {
            newApi.claimToken(ClaimInput(uploadToken.key))
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        val account = try {
            newApi.account()
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        preferences.put(KEY_HSREPLAY_LEGACY_TOKEN, uploadToken.key)
        preferences.put(KEY_HSREPLAY_BATTLETAG, account.battletag)
        preferences.put(KEY_HSREPLAY_USERNAME, account.username)

        return@coroutineScope Result.success(Unit)
    }
}