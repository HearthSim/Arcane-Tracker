package net.mbonnin.arcanetracker.hsreplay;

import net.mbonnin.arcanetracker.hsreplay.model.ClaimResult;
import net.mbonnin.arcanetracker.hsreplay.model.Token;
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest;
import net.mbonnin.arcanetracker.hsreplay.model.Upload;
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;
import rx.Observable;

public interface Service {
    @POST("tokens/")
    Observable<Token> createToken(@Body TokenRequest tokenRequest);

    @POST
    Observable<Upload> createUpload(@Url String url, @Body UploadRequest uploadRequest);

    @POST("claim_account/")
    Observable<ClaimResult> createClaim();

    @GET("tokens/{token}/")
    Observable<Token> getToken(@Path("token") String token);
}
