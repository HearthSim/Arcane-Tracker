package net.hearthsim.hsreplay

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_BATTLETAG
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_LEGACY_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_PREMIUM
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_USERNAME
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput

class HsReplay(val preferences: Preferences, val console: Console, val userAgent: String) {
    private val oauthApi = HsReplayOauthApi(userAgent)
    private val legacyApi = HsReplayLegacyApi(userAgent)
    private val accessTokenProvider = AccessTokenProvider(preferences, oauthApi)
    private val newApi = HsReplayNewApi(userAgent, accessTokenProvider)
    private val s3Api = HsReplayS3Api(userAgent)

    private var account: Account? = null
    private var uploadTokenKey: String? = null

    init {
        uploadTokenKey = preferences.getString(KEY_HSREPLAY_LEGACY_TOKEN)

        val battletag = preferences.getString(KEY_HSREPLAY_BATTLETAG)
        val username = preferences.getString(KEY_HSREPLAY_USERNAME)
        val isPremium = preferences.getBoolean(KEY_HSREPLAY_PREMIUM)

        if (battletag != null && username != null && isPremium != null) {
            account = Account(battletag = battletag,
                    username = username,
                    is_premium = isPremium,
                    id = "")
        }
    }

    fun hasValidAccessToken(): Boolean {
        return accessTokenProvider.isLoggedIn()
    }

    fun account(): Account? {
        /*
         * It might happen that we don't have a valid access token. One possible reason is that the backend had refresh_token rotation activated before mid-may 2019
         * This was removed in https://github.com/HearthSim/HSReplay.net/commit/e1ccc5bf35c73cbc60fa525f737a1aa238cab977#diff-b21dfdea9195d8cf6d85ec5b850218b8
         *
         * But we still have a few users in the wild with an invalid accessToken. If everything goes well, this number should decrease as users reinstall the app
         * and new users come.
         *
         * As of 2019-07-13, it's 2.6k users every 28 days out of 29k.
         */
//        if (!hasValidAccessToken()) {
//            return null
//        }
        if (uploadTokenKey == null) {
            return null
        }
        return account
    }

    suspend fun refreshAccountInformation() {
        try {
            account = newApi.account()
            preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
            preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
            preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)
        } catch (e: Exception) {
            console.error(Exception(e))
        }

    }
    fun logout() {
        accessTokenProvider.forget()
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, null)

        preferences.putString(KEY_HSREPLAY_BATTLETAG, null)
        preferences.putString(KEY_HSREPLAY_USERNAME, null)
        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, null)

        account = null
    }

    suspend fun login(code: String): Result<Unit> = coroutineScope {
        val uploadTokenDeferred = async {
            try {
                legacyApi.createUploadToken()
            } catch (e: Exception) {
                return@async e
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

        try {
            newApi.claimToken(ClaimInput(uploadToken.key))
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        account = try {
            newApi.account()
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        uploadTokenKey = uploadToken.key
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, uploadToken.key)

        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
        preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
        preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)

        return@coroutineScope Result.success(Unit)
    }

    suspend fun uploadGame(uploadRequest: UploadRequest, gameStr: String): Result<String> {
        console.debug("uploadGame [token=$uploadTokenKey]")

        if (uploadTokenKey == null) {
            return Result.failure(Exception("no token"))
        }

        val authorization = "Token $uploadTokenKey"

        val upload = try {
            legacyApi.createUpload(uploadRequest, authorization)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        console.debug("url is ${upload.url}")
        console.debug("put_url is ${upload.put_url}")

        val response = try {
            s3Api.put(putUrl = upload.put_url, gameString = gameStr, userAgent = userAgent)
        } catch (e: Exception) {
            console.error(Exception(e))
            return Result.failure(e)
        }

        if (response.status.value/100 != 2) {
            return Result.failure(Exception("Bad status: ${response.status.value}"))
        }

        return Result.success(upload.url)
    }
}