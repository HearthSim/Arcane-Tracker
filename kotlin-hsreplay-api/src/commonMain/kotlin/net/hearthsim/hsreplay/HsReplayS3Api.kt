package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import net.hearthsim.hsreplay.model.Token

class HsReplayS3Api {
    private val client = HttpClient {
    }

    suspend fun put(putUrl: String, gameString: String, userAgent: String): Token = client.put(putUrl) {
        body = gameString

        header("User-Agent", userAgent)
        header("Content-Type", "text/plain")
    }
}