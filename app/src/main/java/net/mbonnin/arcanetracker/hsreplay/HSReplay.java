package net.mbonnin.arcanetracker.hsreplay;

import com.google.gson.Gson;

import net.mbonnin.arcanetracker.GzipRequestInterceptor;
import net.mbonnin.arcanetracker.ParserListenerPower;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.hsreplay.model.Token;
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest;
import net.mbonnin.arcanetracker.hsreplay.model.Upload;
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest;
import net.mbonnin.arcanetracker.parser.Game;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.util.async.Async;
import timber.log.Timber;

/**
 * Created by martin on 11/29/16.
 */

public class HSReplay {
    private static HSReplay sHSReplay;
    private final OkHttpClient mClient;
    private final OkHttpClient mS3Client;
    private String mToken;
    private Gson mGson = new Gson();

    private final Observer<? super String> mKeyObserver = new Observer<String>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e(e);
        }

        @Override
        public void onNext(String s) {
            mToken = s;
            Settings.set(Settings.HSREPLAY_KEY, s);
        }
    };
    private Observer<? super Void> mUploadObserver = new Observer<Void>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e(e);
        }

        @Override
        public void onNext(Void aVoid) {
            Timber.w("HSREplay upload success");
        }
    };

    public static HSReplay get() {
        if (sHSReplay == null) {
            sHSReplay = new HSReplay();
        }

        return sHSReplay;
    }

    public void uploadGame(String matchStart, String gameStr) {
        if (mToken == null) {
            return;
        }

        Game game = ParserListenerPower.get().getLastGame();
        if (game == null) {
            return;
        }


        Async.start(() -> {
            UploadRequest uploadRequest = new UploadRequest();
            uploadRequest.match_start = matchStart;
            uploadRequest.friendly_player_id = game.player.entity.PlayerID;

            RequestBody body = RequestBody.create(Utils.JSON_MIMETYPE, mGson.toJson(uploadRequest));
            Request request = new Request.Builder()
                    .post(body)
                    .addHeader("Authenticate", "Token " + mToken)
                    .url("https://upload.hsreplay.net/v1/replay/upload/request")
                    .build();

            try {
                Response response = mClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    return null;
                }

                return mGson.fromJson(response.body().string(), Upload.class).put_url;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }).subscribeOn(Schedulers.io())
                .flatMap(putUrl -> {
                    if (putUrl == null) {
                        return null;
                    }

                    return putToS3(putUrl, gameStr);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mUploadObserver);

    }

    public Observable<Void> putToS3(String putUrl, String gameStr) {
        return Async.start(() -> {
            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), gameStr);
            Request request = new Request.Builder()
                    .put(body)
                    .url(putUrl)
                    .build();

            try {
                Response response = mS3Client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public HSReplay() {
        mClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();

                    Request.Builder requestBuilder = request.newBuilder();
                    requestBuilder.addHeader("X-Api-Key", "b70521d3-22e3-43ca-819e-68651fbb9501");
                    request = requestBuilder.build();

                    return chain.proceed(request);
                }).build();

        mS3Client = new OkHttpClient.Builder()
                //.addInterceptor(new GzipRequestInterceptor())
                .build();

        if (false) {
            mToken = Settings.get(Settings.HSREPLAY_KEY, null);
            if (mToken == null) {

                TokenRequest tokenRequest = new TokenRequest();
                tokenRequest.test_data = Utils.isAppDebuggable() ? true : false;

                RequestBody body = RequestBody.create(Utils.JSON_MIMETYPE, mGson.toJson(tokenRequest));
                Request request = new Request.Builder()
                        .post(body)
                        .url("https://hsreplay.net/api/v1/tokens/")
                        .build();

                Async.start(() -> {
                    try {
                        Response response = mClient.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            return null;
                        }

                        return mGson.fromJson(response.body().string(), Token.class).key;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mKeyObserver);
            }
        }
    }
}
