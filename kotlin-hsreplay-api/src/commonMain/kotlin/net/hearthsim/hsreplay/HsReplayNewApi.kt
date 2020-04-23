package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.interceptor.OauthInterceptor
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput
import net.hearthsim.hsreplay.model.new.CollectionUploadRequest
import net.mbonnin.jolly.JollyClient
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.Method

class HsReplayNewApi(val userAgentInterceptor: UserAgentInterceptor, val accessTokenProvider: AccessTokenProvider) {
    companion object {
        const val BASE_URL = "https://api.hsreplay.net/api/v1"
    }

    private val client = JollyClient().apply {
        addInterceptor(OauthInterceptor(accessTokenProvider))
        addInterceptor(userAgentInterceptor)
    }


    suspend fun claimToken(claimInput: ClaimInput): HSReplayResult<*> {
        return JollyRequest {
            method(Method.POST)
            body(ClaimInput.serializer(), claimInput)
            url("$BASE_URL/account/claim_token/")
        }.execute(client)
    }

    suspend fun account(): HSReplayResult<Account> {
        return JollyRequest {
            method(Method.GET)
            url("$BASE_URL/account/")
        }.execute(client, Account.serializer())
    }

    suspend fun collectionUploadRequest(account_hi: String, account_lo: String): HSReplayResult<CollectionUploadRequest> {
        return JollyRequest {
            method(Method.GET)
            url("$BASE_URL/collection/upload_request/?account_hi=$account_hi&account_lo=$account_lo")
        }.execute(client, CollectionUploadRequest.serializer())
    }
}