package net.mbonnin.arcanetracker;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class Deck {
    public static final int MAX_CARDS = 30;

    public Map<String, Integer> cards = new HashMap<>();
    public String name;
    public int classIndex;
    public String id;
    public int wins;
    public int losses;

    private transient WeakReference<Listener> mListenerRef;

    public interface Listener {
        void onDeckChanged();
    }

    public Deck() { }

    public void addCard(String cardId, int add) {
        if (add > 0 && getCardCount() >= 30) {
            return;
        } else if ( add < 0 && getCardCount() <= 0) {
            return;
        }

        Integer a = cards.get(cardId);
        if (a == null) {
            a = 0;
        }

        a += add;

        if (a < 0) {
            return;
        }

        if (a == 0) {
            cards.remove(cardId);
        } else {
            cards.put(cardId, a);
        }

        if (mListenerRef != null) {
            Listener listener = mListenerRef.get();
            if (listener != null) {
                listener.onDeckChanged();
            }
        }
    }

    public int getCardCount() {
        int total = 0;
        for (Integer a: cards.values()) {
            total += a;
        }

        return total;
    }
    public void setListener(Listener listener) {
        mListenerRef = new WeakReference<>(listener);
    }

    public void clear() {
        wins = 0;
        losses = 0;
        cards.clear();
        if (mListenerRef != null) {
            Listener listener = mListenerRef.get();
            if (listener != null) {
                listener.onDeckChanged();
            }
        }
    }
}
