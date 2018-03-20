package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.hsreplay.model.Token
import retrofit2.http.Body
import retrofit2.http.POST
import rx.Observable


interface OauthService {

    @POST("account/claim_token/")
    fun claimToken(@Body token: String): Observable<Token>
}
