package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import net.hearthsim.hsreplay.model.Token
import net.hearthsim.hsreplay.model.legacy.Upload
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken

class HsReplayLegacyApi {
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json.nonstrict).apply {
                setMapper(UploadToken::class, UploadToken.serializer())
                setMapper(UploadRequest::class, UploadRequest.serializer())
            }
        }
    }

    suspend fun createToken(code: String): UploadToken = client.post("https://hsreplay.net/api/v1/tokens")

    suspend fun createUpload(uploadRequest: UploadRequest, authorization: String): Upload = client.post("https://upload.hsreplay.net/api/v1/replay/upload/request") {
        header("Authorization", authorization)
        body = uploadRequest
    }
}