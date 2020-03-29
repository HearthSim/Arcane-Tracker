package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput
import net.hearthsim.hsreplay.model.new.CollectionUploadRequest

class HsReplayNewApi(val userAgent: String, val accessTokenProvider: AccessTokenProvider) {
    companion object {
        const val BASE_URL = "https://api.hsreplay.net/api/v1"
    }

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json(JsonConfiguration(
                ignoreUnknownKeys = true,
                isLenient = true
            ))).apply {
                setMapper(ClaimInput::class, ClaimInput.serializer())
                setMapper(Account::class, Account.serializer())
                setMapper(CollectionUploadRequest::class, CollectionUploadRequest.serializer())
            }
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.ALL
        }

        this.install(OauthFeature.Feature) {
            accessTokenProvider = this@HsReplayNewApi.accessTokenProvider
        }
    }

    suspend fun claimToken(claimInput: ClaimInput): HttpResponse = client.post("$BASE_URL/account/claim_token/") {
        body = claimInput
        contentType(ContentType.Application.Json)
        header("User-Agent", userAgent)
    }

    suspend fun account(): Account = client.get("$BASE_URL/account/") {
        header("User-Agent", userAgent)
    }

    suspend fun collectionUploadRequest(account_hi: String, account_lo: String): CollectionUploadRequest {
        val urlBuilder = URLBuilder("$BASE_URL/collection/upload_request/")
        urlBuilder.parameters.set("account_hi", account_hi)
        urlBuilder.parameters.set("account_lo", account_lo)

        return client.get(urlBuilder.build()) {
            header("User-Agent", userAgent)
        }
    }
}