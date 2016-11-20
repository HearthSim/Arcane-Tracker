package net.mbonnin.arcanetracker;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

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
 * Created by martin on 11/10/16.
 */

public class CardDb {

    public static final String BOOK = "global";
    public static final String KEY_CARDS = "cards";
    private static Object lock = new Object();
    private static ArrayList<Card> sCardList;
    private static String sLanguage;
    private static Observer<? super Config> mConfigObserver = new Observer<Config>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(Config config) {
            int storedVersion = Settings.get(Settings.LOCALE, 0);
            Timber.w("storedVersion=" + storedVersion + " config.cardBd=" + config.cardDb);
            if (config.cardDb != storedVersion) {
                refreshCards();
                Settings.set(Settings.LOCALE, config.cardDb);
            }
        }
    };
    private static boolean sReady;

    static void storeCards(String cards) {
        synchronized (lock) {
            ArrayList<Card> list = new Gson().fromJson(cards, new TypeToken<ArrayList<Card>>() {
            }.getType());
            Collections.sort(list, (a, b) -> a.id.compareTo(b.id));
            sCardList = list;
            sReady = true;
        }
    }

    public static boolean isReady() {
        return sReady;
    }


    private static Observer<String> mCardsObserver = new Observer<String>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(String cards) {
            Paper.book(CardDb.BOOK).write(CardDb.KEY_CARDS + sLanguage, cards);
            CardDb.storeCards(cards);
        }
    };


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
        if (sCardList == null) {
            return new ArrayList<>();
        }
        return sCardList;
    }

    public static void init() {
        String locale = Locale.getDefault().getLanguage().toLowerCase();

        if (locale.contains("fr")) {
            sLanguage = "frFR";
        } else if (locale.contains("ru")) {
            sLanguage = "ruRU";
        } else if (locale.contains("pt")) {
            sLanguage = "ptBR";
        } else {
            sLanguage = "enUS";
        }

        String cards = Paper.book(CardDb.BOOK).read(CardDb.KEY_CARDS + sLanguage);
        if (cards == null) {
            refreshCards();
            return;
        }

        CardDb.storeCards(cards);

        ConfigObservable.get().observeOn(AndroidSchedulers.mainThread()).subscribe(mConfigObserver);
    }

    private static void refreshCards() {
        Async.start(() -> {
                    String endpoint = "https://api.hearthstonejson.com/v1/latest/" + sLanguage + "/cards.json";
                    Timber.w("refreshingCards " + endpoint);
                    Request request = new Request.Builder().url(endpoint).get().build();

                    Response response = null;
                    try {
                        response = new OkHttpClient().newCall(request).execute();
                    } catch (IOException e) {
                        Utils.reportNonFatal(e);
                        return null;
                    }
                    if (response == null || !response.isSuccessful()) {
                        Utils.reportNonFatal(new Exception("cannot get cards"));
                    } else {
                        try {
                            return new String(response.body().bytes());
                        } catch (IOException e) {
                            Timber.e(e);
                        }
                    }
                    return null;
                }
        ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCardsObserver);

    }
}
