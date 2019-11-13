package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.compression.ContentEncoding
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import net.hearthsim.hsreplay.model.Token

class HsReplayS3Api(val userAgent: String) {
    private val client = HttpClient {
        install(GzipCompressFeature)
    }

    suspend fun put(putUrl: String, gameString: ByteArray): HttpResponse = client.put(putUrl) {
        body = ByteArrayContent(gameString, contentType = ContentType.Text.Plain)

        header("User-Agent", userAgent)
    }
}