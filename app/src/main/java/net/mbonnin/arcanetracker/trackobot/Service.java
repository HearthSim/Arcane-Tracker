package net.mbonnin.arcanetracker.trackobot;

import net.mbonnin.arcanetracker.trackobot.model.HistoryList;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

/**
 * Created by martin on 10/25/16.
 */

public interface Service {
    @POST("one_time_auth.json")
    Observable<Url> createOneTimeAuth();

    @POST("users.json")
    Observable<User> createUser();

    @GET("profile/history.json")
    Observable<HistoryList> getHistoryList();

    @POST("profile/results.json")
    Observable<Response<ResultData>> postResults(@Body ResultData resultData);
}
