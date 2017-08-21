package net.mbonnin.arcanetracker.trackobot;

import net.mbonnin.arcanetracker.trackobot.model.HistoryList;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Single;

/**
 * Created by martin on 10/25/16.
 */

public interface Service {
    @POST("one_time_auth.json")
    Single<Url> createOneTimeAuth();

    @POST("users.json")
    Single<User> createUser();

    @GET("profile/history.json")
    Single<HistoryList> getHistoryList();

    @POST("profile/results.json")
    Single<ResultData> postResults(@Body ResultData resultData);
}
