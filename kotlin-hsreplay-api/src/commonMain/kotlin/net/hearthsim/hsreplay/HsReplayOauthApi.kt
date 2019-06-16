package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import net.hearthsim.hsreplay.model.Token

class HsReplayOauthApi {
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json.nonstrict).apply {
                setMapper(Token::class, Token.serializer())
            }
        }
    }

    companion object {
        const val A = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP"
        const val B = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        const val AUTHORIZE_URL = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client"
        const val CALLBACK_URL = "arcanetracker://callback/"
    }

    suspend fun login(code: String): Token = client.post("https://hsreplay.net/oauth2/token/") {
        body = FormDataContent(Parameters.build {
            append("code", code)
            append("client_id", A)
            append("client_secret", B)
            append("grant_type", "authorization_code")
            append("redirect_uri", CALLBACK_URL) // not sure we need redirect_uri but it's working so I'm leaving it
        })
    }

    suspend fun refresh(refreshToken: String): HttpResponse = client.post("https://hsreplay.net/oauth2/token/") {
        body = FormDataContent(Parameters.build {
            append("client_id", A)
            append("client_secret", B)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        })
    }
}