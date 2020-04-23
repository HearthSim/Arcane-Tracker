package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.interceptor.GzipInterceptor
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import net.mbonnin.jolly.JollyClient
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.JollyResponse
import net.mbonnin.jolly.Method

class HSReplayS3CollectionApi(val userAgentInterceptor: UserAgentInterceptor) {
    private val client = JollyClient().apply {
        addInterceptor(userAgentInterceptor)
    }

    suspend fun put(putUrl: String, collectionUploadData: CollectionUploadData): HSReplayResult<JollyResponse> {
        return JollyRequest {
            method(Method.PUT)
            body(CollectionUploadData.serializer(), collectionUploadData)
            url(putUrl)

        }.execute(client)
    }
}