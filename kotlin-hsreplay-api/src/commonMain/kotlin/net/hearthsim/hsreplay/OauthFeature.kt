package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.accept
import io.ktor.client.response.HttpResponseContainer
import io.ktor.client.response.HttpResponsePipeline
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.readRemaining

class OauthFeature {
    class Config {
    }

    companion object Feature : HttpClientFeature<Config, OauthFeature> {
        override val key: AttributeKey<OauthFeature> = AttributeKey("Oauth")

        override fun prepare(block: OauthFeature.Config.() -> Unit): OauthFeature {
            val config = OauthFeature.Config().apply(block)

            return OauthFeature()
        }

        override fun install(feature: OauthFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Send) { payload ->
                context.headers.remove(HttpHeaders.ContentType)

                val serializedContent = when (payload) {
                    is EmptyContent -> feature.serializer.write(Unit, contentType)
                    else -> feature.serializer.write(payload, contentType)
                }

                val subject = proceedWith(serializedContent)
                subject
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Transform) { (info, body) ->
                if (body !is ByteReadChannel) return@intercept

                if (feature.acceptContentTypes.none { context.response.contentType()?.match(it) == true })
                    return@intercept
                try {
                    proceedWith(HttpResponseContainer(info, feature.serializer.read(info, body.readRemaining())))
                } finally {
                    context.close()
                }
            }
        }
    }


}