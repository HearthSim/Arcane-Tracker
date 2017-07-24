package net.mbonnin.arcanetracker.hsreplay;

import android.os.Build;

import com.google.gson.Gson;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.BuildConfig;
import net.mbonnin.arcanetracker.MainViewCompanion;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.hsreplay.model.Token;
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest;
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest;
import net.mbonnin.arcanetracker.model.GameSummary;
import net.mbonnin.arcanetracker.parser.Game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by martin on 11/29/16.
 */

public class HSReplay {
    private static final java.lang.String KEY_GAME_LIST = "KEY_GAME_LIST";
    private static HSReplay sHSReplay;
    private final OkHttpClient mS3Client;
    private final String mUserAgent;
    private String mToken;
    private ArrayList<GameSummary> mGameList;
    private Service mService;

    private final Observer<Token> mTokenObserver = new Observer<Token>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e("createTokenError" + e);
        }

        @Override
        public void onNext(Token token) {
            mToken = token.key;
            Timber.w("got token=" + mToken);
            Settings.set(Settings.HSREPLAY_TOKEN, mToken);
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

    public ArrayList<GameSummary> getGameSummary() {
        return mGameList;
    }

    public void uploadGameDebug(String matchStart, String friendlyPlayerId, GameSummary summary, String gameStr) {
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.match_start = matchStart;
        uploadRequest.build = 20022;
        uploadRequest.friendly_player_id = friendlyPlayerId;
        uploadRequest.game_type = summary.bnetGameType;

        service().createUpload("https://upload.hsreplay.net/api/v1/replay/upload/request", uploadRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(upload -> {
                    Timber.w("url is " + upload.url);
                    Timber.w("put_url is " + upload.put_url);

                    if (upload.put_url == null) {
                        return null;
                    }

                    if (summary != null) {
                        summary.hsreplayUrl = upload.url;
                        Paper.book().write(KEY_GAME_LIST, mGameList);
                    }
                    return putToS3(upload.put_url, gameStr).subscribeOn(Schedulers.io());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mUploadObserver);
    }

    public void uploadGame(String matchStart, Game game, String gameStr) {
        Timber.w("uploadGame");
        if (game == null) {
            return;
        }

        GameSummary summary = new GameSummary();
        summary.coin = game.getPlayer().hasCoin;
        summary.win = game.victory;
        summary.hero = game.player.classIndex();
        summary.opponentHero = game.opponent.classIndex();
        summary.date = Utils.ISO8601DATEFORMAT.format(new Date());
        summary.deckName = MainViewCompanion.getPlayerCompanion().getDeck().name;
        summary.bnetGameType = game.bnetGameType;

        mGameList.add(0, summary);
        Paper.book().write(KEY_GAME_LIST, mGameList);

        if (mToken == null) {
            return;
        }

        if (Settings.get(Settings.HSREPLAY, Settings.DEFAULT_HSREPLAY)) {
            uploadGameDebug(matchStart, game.player.entity.PlayerID, summary, gameStr);
        }
    }

    public Observable<Void> putToS3(String putUrl, String gameStr) {
        return Observable.fromCallable(() -> {
            RequestBody body = RequestBody.create(null, gameStr);
            Request request = new Request.Builder()
                    .put(body)
                    .url(putUrl)
                    .header("Content-Type", "text/plain")
                    .addHeader("User-Agent", mUserAgent)
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
        mGameList = Paper.book().read(KEY_GAME_LIST);
        if (mGameList == null) {
            mGameList = new ArrayList<>();
        }

        mUserAgent = ArcaneTrackerApplication.getContext().getPackageName() + "/" + BuildConfig.VERSION_NAME
                + "; Android " + Build.VERSION.RELEASE + "; " + String.format("%.3f", Utils.getDiagonal()) + "inches";

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();

                    Request.Builder requestBuilder = request.newBuilder();
                    requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3");
                    requestBuilder.addHeader("User-Agent", mUserAgent);
                    if (mToken != null) {
                        requestBuilder.addHeader("Authorization", "Token " + mToken);
                    }
                    request = requestBuilder.build();

                    return chain.proceed(request);
                }).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://hsreplay.net/api/v1/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(client)
                .build();

        mService = retrofit.create(Service.class);

        mS3Client = new OkHttpClient.Builder()
                //.addInterceptor(new GzipRequestInterceptor())
                .build();

        if (Settings.get(Settings.HSREPLAY, Settings.DEFAULT_HSREPLAY)) {
            mToken = Settings.get(Settings.HSREPLAY_TOKEN, null);

            Timber.w("init token=" + mToken);

            if (mToken == null) {
                generateToken();
            }
        }

        //uploadGameDebug(Utils.ISO8601DATEFORMAT.format(new Date()), "1", null, "toto");
    }

    private void generateToken() {
        TokenRequest tokenRequest = new TokenRequest();
        tokenRequest.test_data = Utils.isAppDebuggable() ? true : false;

        service().createToken(tokenRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mTokenObserver);
    }

    public String getToken() {
        return mToken;
    }

    public void eraseGameSummary() {
        mGameList.clear();
        mToken = null;
        Settings.set(Settings.HSREPLAY_TOKEN, null);
        Paper.book().write(KEY_GAME_LIST, mGameList);

        generateToken();
    }

    public Service service() {
        return mService;
    }
}
