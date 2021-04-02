package net.hearthsim.hsreplay

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import net.mbonnin.jolly.JollyClient
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.JollyResponse
import okio.buffer
import okio.internal.commonToUtf8String

suspend fun JollyRequest.execute(client: JollyClient? = null): HSReplayResult<JollyResponse> {
    val client = client ?: JollyClient()

    val response = client.execute(this)

    if (response.error != null) {
        return HSReplayResult.Error(response.error)
    }
    if (response.statusCode / 100 != 2) { // 201 is a valid statusCode and happens when we create a new token
        return HSReplayResult.Error(Exception("HTTP error: ${response.statusCode}:\n${response.body?.commonToUtf8String()}"))
    }

    return HSReplayResult.Success(response)
}

internal fun JollyRequest.addHeader(key: String, value: String?): JollyRequest {
    val newHeaders = headers.toMutableMap().apply {
        put(key, value.toString())
    }

    return this.copy(headers = newHeaders)
}

suspend fun <T> JollyRequest.execute(client: JollyClient? = null, serializer: DeserializationStrategy<T>): HSReplayResult<T> {
    val client = client ?: JollyClient()

    val result = execute(client = client)

    if (result !is HSReplayResult.Success) {
        return result as HSReplayResult<T>
    }

    val token = try {
        val json = Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
            isLenient = true
        }
        json.decodeFromString(serializer, result.value.body?.commonToUtf8String() ?: "")
    } catch (e: Exception) {
        return HSReplayResult.Error(Exception("Json parse exception", e))
    }

    return HSReplayResult.Success(token)
}