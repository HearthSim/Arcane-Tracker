package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.features.HttpSend
import io.ktor.client.features.feature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey

class OauthFeature(val accessTokenProvider: AccessTokenProvider) {
    suspend private fun accessToken() {
        accessTokenProvider.accessToken()
    }

    suspend private fun refreshToken() {
        accessTokenProvider.refreshToken()
    }

    class Config {
        lateinit var accessTokenProvider: AccessTokenProvider
    }

    companion object Feature : HttpClientFeature<Config, OauthFeature> {
        override val key: AttributeKey<OauthFeature> = AttributeKey("Oauth")

        override fun prepare(block: Config.() -> Unit): OauthFeature {
            val config = Config().apply(block)

            return OauthFeature(config.accessTokenProvider)
        }

        override fun install(feature: OauthFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.headers.append(HttpHeaders.Authorization, "Bearer ${feature.accessToken()}")
            }

            scope.feature(HttpSend)!!.intercept { origin ->
                var call = origin

                while (call.response.status == HttpStatusCode.Unauthorized) {
                    feature.refreshToken()

                    val request = HttpRequestBuilder()
                    request.takeFrom(call.request)
                    request.headers.append(HttpHeaders.Authorization, "Bearer ${feature.accessToken()}")

                    call.close()
                    call = execute(request)
                }

                return@intercept call
            }
        }
    }
}