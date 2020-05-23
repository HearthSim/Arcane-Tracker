package net.hearthsim.hsreplay

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_BATTLETAG
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_LEGACY_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_PREMIUM
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_USERNAME
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput
import net.hearthsim.hsreplay.model.new.CollectionUploadData

class HsReplay(
        val preferences: Preferences,
        val console: Console,
        private val accessTokenProvider: AccessTokenProvider,
        userAgent: String) {
    val userAgentInterceptor = UserAgentInterceptor(userAgent)
    private val legacyApi = HsReplayLegacyApi(userAgentInterceptor)
    private val newApi = HsReplayNewApi(userAgentInterceptor, accessTokenProvider)
    private val s3Api = HsReplayS3GameApi(userAgentInterceptor)

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

    suspend fun claimToken(uploadToken: String): Boolean {
        return when(newApi.claimToken(ClaimInput(uploadToken))) {
            is HSReplayResult.Success -> true
            is HSReplayResult.Error -> false
        }
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

    suspend fun refreshAccountInformation(): HSReplayResult<Account> {
        console.debug("getting account information")
        val result = newApi.account()
        if (result !is HSReplayResult.Success) {
            console.error(Exception("cannot get account:", (result as HSReplayResult.Error).exception))
            return result
        }
        account = result.value

        console.debug("got account: ${account!!.battletag}")

        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
        preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
        preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)

        return result
    }

    fun logout() {
        accessTokenProvider.logout()
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, null)

        preferences.putString(KEY_HSREPLAY_BATTLETAG, null)
        preferences.putString(KEY_HSREPLAY_USERNAME, null)
        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, null)

        account = null
    }

    sealed class LoginResult {
        object Success : LoginResult()
        class Failure(val e: Throwable) : LoginResult()
    }

    /**
     * An optimisation with login() that will create the uploadToken and login in parallel
     */
    suspend fun loginWithCode(code: String): LoginResult {
        return loginInternal { accessTokenProvider.login(code) }
    }

    /**
     * A helper method to:
     * - Create an upload token
     * - Claim it
     * - Retrieve account info
     * - Store everything in cache
     */
    suspend fun login(): LoginResult {
        return loginInternal { HSReplayResult.Success(Unit) }
    }

    private suspend fun loginInternal(login: suspend () -> HSReplayResult<Unit>) = coroutineScope {
        val uploadTokenResultDeferred = async {
            legacyApi.createUploadToken()
        }

        val loginResult = login()

        val uploadTokenResult = uploadTokenResultDeferred.await()
        if (uploadTokenResult !is HSReplayResult.Success) {
            return@coroutineScope LoginResult.Failure((uploadTokenResult as HSReplayResult.Error).exception)
        }

        if (loginResult !is HSReplayResult.Success) {
            return@coroutineScope LoginResult.Failure((loginResult as HSReplayResult.Error).exception)
        }

        val uploadToken = uploadTokenResult.value

        val claimTokenResult = newApi.claimToken(ClaimInput(uploadToken.key))
        if (claimTokenResult !is HSReplayResult.Success) {
            accessTokenProvider.logout()
            return@coroutineScope LoginResult.Failure((claimTokenResult as HSReplayResult.Error).exception)
        }

        val accountResult = newApi.account()
        if (accountResult !is HSReplayResult.Success) {
            accessTokenProvider.logout()
            return@coroutineScope LoginResult.Failure((accountResult as HSReplayResult.Error).exception)
        }

        account = accountResult.value

        uploadTokenKey = uploadToken.key
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, uploadToken.key)

        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
        preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
        preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)

        return@coroutineScope LoginResult.Success
    }

    sealed class UploadResult {
        class Success(val replayUrl: String) : UploadResult()
        class Failure(val e: Throwable) : UploadResult()
    }

    suspend fun uploadGame(uploadRequest: UploadRequest, gameStr: ByteArray): UploadResult {
        console.debug("uploadGame [token=$uploadTokenKey]")

        if (uploadTokenKey == null) {
            return UploadResult.Failure(Exception("no token"))
        }

        val authorization = "Token $uploadTokenKey"

        val uploadResult = legacyApi.createUpload(uploadRequest, authorization)

        if (uploadResult !is HSReplayResult.Success) {
            return UploadResult.Failure((uploadResult as HSReplayResult.Error).exception)
        }

        val upload = uploadResult.value

        console.debug("url is ${upload.url}")
        console.debug("put_url is ${upload.put_url}")

        val s3Result = s3Api.put(putUrl = upload.put_url, gameString = gameStr)

        if (s3Result !is HSReplayResult.Success) {
            val e = (s3Result as HSReplayResult.Error).exception
            console.error(Exception(e))
            return UploadResult.Failure(e)
        }

        return UploadResult.Success(upload.url)
    }

    sealed class CollectionUploadResult {
        object Success : CollectionUploadResult()
        class Failure(val code: Int, val throwable: Throwable) : CollectionUploadResult()
    }

    suspend fun uploadCollection(collectionUploadData: CollectionUploadData, account_hi: String, account_lo: String): CollectionUploadResult {
        // Get the account first to ensure a valid access token else the collection upload route returns 400 instead of 401
        val accountResult = newApi.account()
        if (accountResult is HSReplayResult.Error) {
            return CollectionUploadResult.Failure(101, accountResult.exception)
        }

        val uploadCollectionRequestResult = newApi.collectionUploadRequest(account_hi, account_lo)
        if (uploadCollectionRequestResult !is HSReplayResult.Success) {
            return CollectionUploadResult.Failure(102, (uploadCollectionRequestResult as HSReplayResult.Error).exception)
        }

        val s3CollectionApi = HSReplayS3CollectionApi(userAgentInterceptor)

        val uploadResult = s3CollectionApi.put(uploadCollectionRequestResult.value.url, collectionUploadData)

        if (uploadResult !is HSReplayResult.Success) {
            return CollectionUploadResult.Failure( 103, (uploadResult as HSReplayResult.Error).exception)
        }

        return CollectionUploadResult.Success
    }
}