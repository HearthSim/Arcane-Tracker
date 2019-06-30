package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.compression.ContentEncoding
import io.ktor.client.request.header
import io.ktor.client.request.put
import net.hearthsim.hsreplay.model.Token

class HsReplayS3Api(val userAgent: String) {
    private val client = HttpClient {
        install(ContentEncoding) {
            gzip()
        }

    }

    suspend fun put(putUrl: String, gameString: String, userAgent: String): Token = client.put(putUrl) {
        body = gameString

        header("User-Agent", userAgent)
    }
}