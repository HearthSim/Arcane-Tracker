package net.mbonnin.arcanetracker.adapter;

import android.text.TextUtils;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.CardEntity;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.Player;

import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/8/16.
 */

public class PlayerController implements Player.Listener {
    private Player mPlayer;
    private Deck mDeck;
    private final DeckAdapter mAdapter;

    public PlayerController(DeckAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void onPlayerStateChanged() {
        update();
    }

    private void update() {
        if (mDeck == null) {
            return;
        }

        ArrayList<Entity> activeCards;
        if (mPlayer == null) {
            activeCards = new ArrayList<>();
        } else {
            activeCards = mPlayer.entities;
        }
        /**
         * add cards to the deck if needed
         */
        if (Settings.get(Settings.AUTO_ADD_CARDS, true) && Utils.cardMapTotal(mDeck.cards) >= Deck.MAX_CARDS) {
            HashMap<String, Integer> knownCards = Utils.filterCollectibleCards(activeCards);

            /**
             * now add the cards we know to the deck if needed
             */
            for (String cardId : knownCards.keySet()) {
                int found = knownCards.get(cardId);
                if (found > Utils.cardMapGet(mDeck.cards, cardId)) {
                    Timber.w("adding card to the deck " + cardId);
                    Utils.cardMapAdd(mDeck.cards, cardId, 1);
                }
            }
        }

        HashMap<String, Integer> map = new HashMap<String, Integer>(mDeck.cards); // we need to deep copy so we don't modify the original deck
        for (Entity cardEntity : activeCards) {
            if (!Card.isCollectible(cardEntity.CardID)) {
                continue;
            }

            String cardId = cardEntity.CardID;
            if (TextUtils.isEmpty(cardId)) {
                continue;
            }

            if (!"DECK".equals(cardEntity.tags.get("ZONE"))) {
                Utils.cardMapAdd(map, cardId, -1);
            }
        }

        ArrayList list = Utils.cardMapToBarItems(map);

        int total = Utils.cardMapTotal(mDeck.cards);
        if (total < Deck.MAX_CARDS) {
            list.add(String.format("%d unknown card(s)", Deck.MAX_CARDS - total));
        }

        mAdapter.setList(list);
    }

    public void setDeck(Deck deck, Player player) {
        mDeck = deck;
        if (mPlayer != null) {
            mPlayer.listeners.remove(this);
        }
        mPlayer = player;
        if (mPlayer != null) {
            mPlayer.registerListener(this);
        }
        update();
    }
}
