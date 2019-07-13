package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey

expect object GzipEncoder {
    fun encode(bytes: ByteArray): ByteArray
}

class GzipCompressFeature {
    companion object Feature : HttpClientFeature<Unit, GzipCompressFeature> {
        override val key: AttributeKey<GzipCompressFeature> = AttributeKey("GzipCompress")

        override fun prepare(block: Unit.() -> Unit): GzipCompressFeature {
            return GzipCompressFeature()
        }

        override fun install(feature: GzipCompressFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Transform) {payload ->
                context.headers.append(HttpHeaders.ContentEncoding, "gzip")

                val bytes = when (payload) {
                    is OutgoingContent.ByteArrayContent -> payload.bytes()
                    else -> throw Exception("This is not supported")
                }
                val contentType = payload.contentType

                proceedWith(ByteArrayContent(bytes = GzipEncoder.encode(bytes), contentType = contentType))
            }
        }
    }
}