package net.mbonnin.arcanetracker.hsreplay

import io.reactivex.Observable
import net.mbonnin.arcanetracker.hsreplay.model.Account
import net.mbonnin.arcanetracker.hsreplay.model.Token
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface OauthService {

    @POST("account/claim_token/")
    fun claimToken(@Body tokenBody: RequestBody): Observable<Token>

    @GET("account/")
    fun account(): Observable<Account>
}
