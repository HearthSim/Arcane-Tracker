package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.hsreplay.model.Account
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadToken
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface HsReplayService {

    @POST("account/claim_token/")
    fun claimToken(@Body tokenBody: RequestBody): Call<UploadToken>

    @GET("account/")
    fun account(): Call<Account>
}
