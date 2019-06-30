package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.hearthsim.hsreplay.model.legacy.Upload
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken

class HsReplayLegacyApi(val userAgent: String) {
    private val client = HttpClient {
        val json = Json(
                JsonConfiguration(
                        encodeDefaults = false,
                        strictMode = false
                )
        )
        install(JsonFeature) {
            serializer = KotlinxSerializer(json).apply {
                setMapper(UploadToken::class, UploadToken.serializer())
                setMapper(UploadRequest::class, UploadRequest.serializer())
            }
        }
    }

    suspend fun createUploadToken(): UploadToken = client.post("https://hsreplay.net/api/v1/tokens/") {
        body = object : OutgoingContent.WriteChannelContent() {
            override suspend fun writeTo(channel: ByteWriteChannel) {
                channel.writeStringUtf8("{}")
            }

            override val contentType: ContentType?
                get() = ContentType.parse("application/json")
        }

        header("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
        header("User-Agent", userAgent)
    }

    suspend fun createUpload(uploadRequest: UploadRequest, authorization: String): Upload = client.post("https://upload.hsreplay.net/api/v1/replay/upload/request") {
        header("Authorization", authorization)
        body = uploadRequest
        contentType(ContentType.Application.Json)

        header("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
        header("User-Agent", userAgent)
    }
}