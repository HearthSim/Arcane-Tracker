package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.hsreplay.model.legacy.Upload
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadToken
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadTokenRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface LegacyService {
    @POST("tokens/")
    fun createToken(@Body uploadTokenRequest: UploadTokenRequest): Call<UploadToken>

    @POST
    fun createUpload(@Url url: String, @Body uploadRequest: UploadRequest, @Header("Authorization") authorization: String): Call<Upload>
}
