package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.hsreplay.model.Token
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest
import net.mbonnin.arcanetracker.hsreplay.model.Upload
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest

import io.reactivex.Observable
import retrofit2.http.*

interface LegacyService {
    @POST("tokens/")
    fun createToken(@Body tokenRequest: TokenRequest): Observable<Token>

    @POST
    fun createUpload(@Url url: String, @Body uploadRequest: UploadRequest, @Header("Authorization") authorization: String): Observable<Upload>

    @GET("tokens/{token}/")
    fun getToken(@Path("token") token: String): Observable<Token>
}
