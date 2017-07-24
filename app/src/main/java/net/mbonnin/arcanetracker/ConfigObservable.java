package net.mbonnin.arcanetracker;

import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;
import timber.log.Timber;

/**
 * Created by martin on 11/10/16.
 */

public class ConfigObservable {
    static Observable<Config> get() {
        ReplaySubject<Config> subject = ReplaySubject.create();

        Observable.fromCallable(() -> {
            Request request = new Request.Builder().url("https://arcanetracker.com/config.json").get().build();

            Response response = null;
            try {
                response = new OkHttpClient().newCall(request).execute();
            } catch (IOException e) {
                Timber.e(e);
                return null;
            }
            if (response == null || !response.isSuccessful()) {
            } else {
                try {
                    return new Gson().fromJson(response.body().string(), Config.class);
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
            return null;

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subject);

        return subject;

    }
}
