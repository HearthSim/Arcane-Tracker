package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput

class HsReplayNewApi(val userAgent: String, val accessTokenProvider: AccessTokenProvider) {
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json.nonstrict).apply {
                setMapper(ClaimInput::class, ClaimInput.serializer())
                setMapper(Account::class, Account.serializer())
            }
        }
        this.install(OauthFeature.Feature) {
            accessTokenProvider = this@HsReplayNewApi.accessTokenProvider
        }
    }

    suspend fun claimToken(claimInput: ClaimInput): HttpResponse = client.post("https://api.hsreplay.net/v1/account/claim_token/") {
        body = claimInput
        contentType(ContentType.Application.Json)
        header("User-Agent", userAgent)
    }

    suspend fun account(): Account = client.get("https://api.hsreplay.net/v1/account/") {
        header("User-Agent", userAgent)
    }
}