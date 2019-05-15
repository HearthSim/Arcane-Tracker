package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.hsreplay.model.Account
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadToken
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface HsReplayService {

    // We need the response even if we don't read it else retrofit returns an error because the response is empty
    // We don't use Call either because the retrofit coroutine extension will choken on an empty body
    @POST("account/claim_token/")
    fun claimToken(@Body tokenBody: RequestBody): Call<Response<UploadToken>>

    @GET("account/")
    fun account(): Call<Account>
}
