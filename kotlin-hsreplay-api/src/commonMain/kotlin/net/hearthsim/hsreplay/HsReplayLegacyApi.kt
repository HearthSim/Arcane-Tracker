package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.interceptor.GzipInterceptor
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.legacy.Upload
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken
import net.mbonnin.jolly.JollyClient
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.Method

@OptIn
class HsReplayLegacyApi(val userAgentInterceptor: UserAgentInterceptor) {
    val apiKey = "8b27e53b-0256-4ff1-b134-f531009c05a3"

    private val client = JollyClient().apply {
        addInterceptor(userAgentInterceptor)
    }

    suspend fun createUploadToken(): HSReplayResult<UploadToken> {
        return JollyRequest {
            method(Method.POST)
            addHeader("X-Api-Key", apiKey)
            body("application/json", "{}")
            url("https://hsreplay.net/api/v1/tokens/")
        }.execute(client, UploadToken.serializer())
    }

    suspend fun createUpload(uploadRequest: UploadRequest, authorization: String): HSReplayResult<Upload>  {
        return JollyRequest {
            method(Method.POST)
            addHeader("X-Api-Key", apiKey)
            addHeader("Authorization", authorization)
            body(UploadRequest.serializer(), uploadRequest)
            url("https://upload.hsreplay.net/api/v1/replay/upload/request")
        }.execute(client, Upload.serializer())
    }
}