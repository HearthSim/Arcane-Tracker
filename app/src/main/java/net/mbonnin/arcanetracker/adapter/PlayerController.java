package net.mbonnin.arcanetracker.adapter;

import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.DeckList;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 11/8/16.
 */

public class PlayerController implements Player.Listener, Deck.Listener {
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

        EntityList entities;
        if (mPlayer == null) {
            entities = new EntityList();
        } else {
            entities = mPlayer.entities;
        }

        /**
         * add cards to the deck if:
         *
         *    * the setting is enabled
         *    * the deck is less than 30 cards
         */
        EntityList originalDeck = entities.filter(EntityList.IS_FROM_ORIGINAL_DECK);
        HashMap<String, Integer> originalDeckMap = originalDeck.toCardMap();
        if (Settings.get(Settings.AUTO_ADD_CARDS, true) && Utils.cardMapTotal(mDeck.cards) < Deck.MAX_CARDS) {
            for (String cardId : originalDeckMap.keySet()) {
                int found = originalDeckMap.get(cardId);
                if (found > Utils.cardMapGet(mDeck.cards, cardId)) {
                    Timber.w("adding card to the deck " + cardId);
                    mDeck.cards.put(cardId, found);
                }
            }
            DeckList.save();
        }

        /**
         * now we take the deck and remove the cards we know have been played
         */
        EntityList originalDeckPlayed = originalDeck.filter(EntityList.IS_OUTSIDE_DECK);
        HashMap<String, Integer> remainingCardMap = Utils.cardMapDiff(mDeck.cards, originalDeckPlayed.toCardMap());

        ArrayList<BarItem> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : remainingCardMap.entrySet()) {
            BarItem deckEntry = new BarItem();
            deckEntry.card = CardDb.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            list.add(deckEntry);
        }

        EntityList createdCards = entities.filter(EntityList.IS_IN_DECK)
                .filter(EntityList.IS_NOT_FROM_ORIGINAL_DECK)
                .filter(EntityList.HAS_CARD_ID);
        for (Entity entity : createdCards) {
            BarItem deckEntry = new BarItem();
            deckEntry.card = CardDb.getCard(entity.CardID);
            deckEntry.count = 1;
            deckEntry.gift = true;
            list.add(deckEntry);
        }

        Collections.sort(list, BarItem.COMPARATOR);

        int total = Utils.cardMapTotal(mDeck.cards);
        if (total < Deck.MAX_CARDS) {
            /**
             * I'm not really sure how come this cast is working....
             */
            ArrayList list2 = list;
            list2.add(String.format("%d unknown card(s)", Deck.MAX_CARDS - total));
        }

        mAdapter.setList(list);
    }

    public void setDeck(Deck deck, Player player) {
        deck.setListener(this);

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

    @Override
    public void onDeckChanged() {
        update();
    }
}
