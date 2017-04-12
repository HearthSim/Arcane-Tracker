package net.mbonnin.arcanetracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;

import io.paperdb.Book;
import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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
    private final Book book;
    private final Settings settings;
    private List<Card> sCardList;
    private String sLanguage;
    private boolean sReady;

    void storeCards(String cards) {
        synchronized (lock) {
            ArrayList<Card> list = new Gson().fromJson(cards, new TypeToken<ArrayList<Card>>() {
            }.getType());
            Collections.sort(list, (a, b) -> a.id.compareTo(b.id));
            sCardList = list;
            sReady = true;
        }
    }

    public boolean isReady() {
        return sReady;
    }

    public Card getCard(String key) {
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

    public List<Card> getCards() {
        if (sCardList == null) {
            return new ArrayList<>();
        }
        return sCardList;
    }

    public CardDb(@Named(CardDb.BOOK) Book book, Settings settings) {
        this.book = book;
        this.settings = settings;
        String locale = Locale.getDefault().getLanguage().toLowerCase();

        if (locale.contains("fr")) {
            sLanguage = "frFR";
        } else if (locale.contains("ru")) {
            sLanguage = "ruRU";
        } else if (locale.contains("pt")) {
            sLanguage = "ptBR";
        }else if (locale.contains("ko")) {
            sLanguage = "koKR";
        } else {
            sLanguage = "enUS";
        }

        String cards = book.read(CardDb.KEY_CARDS + sLanguage);
        if (cards == null) {
            refreshCards();
            return;
        }

        storeCards(cards);

        ConfigObservable.get().observeOn(AndroidSchedulers.mainThread()).subscribe(config -> {
            int storedVersion = settings.get(Settings.LOCALE, 0);
            Timber.w("storedVersion=" + storedVersion + " config.cardBd=" + config.cardDb);
            if (config.cardDb != storedVersion) {
                refreshCards();
                settings.set(Settings.LOCALE, config.cardDb);
            }
        });
    }

    private void refreshCards() {
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
            .subscribe(cards -> {
                book.write(CardDb.KEY_CARDS + sLanguage, cards);
                storeCards(cards);
            });
    }

    public void checkClassIndex(Deck deck) {
        for (String cardId: deck.cards.keySet()) {
            Card card = getCard(cardId);
            int ci = Card.playerClassToClassIndex(card.playerClass);
            if (ci >= 0 && ci < Card.CLASS_INDEX_NEUTRAL) {
                if (deck.classIndex != ci) {
                    Timber.e("inconsistent class index, force to" + Card.classIndexToPlayerClass(ci));
                    deck.classIndex = ci;
                }
                return;
            }
        }
    }
}
