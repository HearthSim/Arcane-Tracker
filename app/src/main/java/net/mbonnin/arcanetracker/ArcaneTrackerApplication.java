package net.mbonnin.arcanetracker;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.util.async.Async;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneTrackerApplication extends Application {
    private static ArcaneTrackerApplication sApplication;
    private static Context sContext;

    public static final String BOOK = "global";
    public static final String KEY_CARDS = "cards";


    private Request request;

    private static Object lock = new Object();
    private static ArrayList<Card> sCardList;

    private Observer<String> mCardsObserver = new Observer<String>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(String cards) {
            Paper.book(BOOK).write(KEY_CARDS, cards);
            setupCards(cards);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        sContext = new ContextThemeWrapper(this, R.style.AppThemeLight);

        Timber.plant(new Timber.DebugTree());
        Paper.init(this);

        if (false) {
            Picasso.with(this)
                    .setIndicatorsEnabled(true);
        }

        StopServiceBroadcastReceiver.init();
        SettingsBroadcastReceiver.init();

        String cards = Paper.book(BOOK).read(KEY_CARDS);
        if (cards != null) {
            setupCards(cards);
        }
        Async.start(() -> {
                    String endpoint = "https://api.hearthstonejson.com/v1/latest/enUS/cards.json";
                    request = new Request.Builder().url(endpoint).get().build();

                    Response response = null;
                    try {
                        response = new OkHttpClient().newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    if (response == null || !response.isSuccessful()) {
                        Toast.makeText(ArcaneTrackerApplication.this, getString(R.string.cannot_get_latest_cards), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            return new String(response.body().bytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
        ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCardsObserver);

        Parser.get();

    }

    private void setupCards(String cards) {
        synchronized (lock) {
            ArrayList<Card> list = new Gson().fromJson(cards, new TypeToken<ArrayList<Card>>() {}.getType());
            Collections.sort(list, (a, b) -> a.id.compareTo(b.id));
            sCardList = list;
        }
    }

    public static Context getContext() {
        return sContext;
    }

    public static Card getCard(String key) {
        synchronized (lock) {
            if (sCardList == null) {
                /**
                 * can happen the very first launch
                 */
                return Card.unknown();
            }
            int index = Collections.binarySearch(sCardList, key);
            if (index < 0) {
                return Card.unknown();
            } else {
                return sCardList.get(index);
            }
        }
    }

    public static ArrayList<Card> getCards() {
        return sCardList;
    }
}
