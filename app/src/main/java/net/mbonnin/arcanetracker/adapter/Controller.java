package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.text.TextUtils;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.DeckList;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 11/21/16.
 */
public class Controller implements GameLogic.Listener {
    private static Controller sPlayerController;
    private static Controller sOpponentController;

    private final boolean mOpponent;
    private final ItemAdapter mAdapter;
    protected Deck mDeck;
    private Game mGame;

    public Controller(boolean opponent) {
        mOpponent = opponent;
        mAdapter = new ItemAdapter();
        GameLogic.get().addListener(this);
    }

    public ItemAdapter getAdapter() {
        return mAdapter;
    }

    public void setDeck(Deck deck) {
        mDeck = deck;
        update();
    }

    private ArrayList getDeck() {
        EntityList originalDeck;
        if (mGame == null) {
            originalDeck = new EntityList();

            for (int i = 0; i < Deck.MAX_CARDS; i++) {
                Entity entity = new Entity();
                entity.EntityID = Integer.toString(i);
                entity.tags.put(Entity.KEY_ZONE, Entity.ZONE_DECK);
                originalDeck.add(entity);
            }
        } else {
            String playerId = mOpponent ? mGame.opponent.entity.PlayerID:mGame.player.entity.PlayerID;
            originalDeck = mGame.getEntityList(entity -> playerId.equals(entity.extra.originalController));
        }

        /**
         * give a card Id to the unknown cards in the deck
         */
        if (mDeck != null) {
            ArrayList<String> cardIdsFromDeck = new ArrayList<>();
            for (Map.Entry<String, Integer> entry: mDeck.cards.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    cardIdsFromDeck.add(entry.getKey());
                }
            }

            /**
             * 1st pass: remove the known card ids
             */
            for (Entity entity: originalDeck) {
                if (!TextUtils.isEmpty(entity.CardID)) {
                    Iterator<String> it = cardIdsFromDeck.iterator();
                    while(it.hasNext()) {
                        String next = it.next();
                        if (next.equals(entity.CardID)) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
            /**
             * 2nd pass: assign a cardId to the cards we still don't know
             */
            int i = 0;
            for (Entity entity: originalDeck) {
                if (TextUtils.isEmpty(entity.CardID)) {
                    if (i < cardIdsFromDeck.size()) {
                        entity.extra.tmpCardId = cardIdsFromDeck.get(i);
                        i++;
                    }
                } else {
                    entity.extra.tmpCardId = entity.CardID;
                }
            }
        }

        /**
         * remove the unknown cards and count them
         */
        Iterator<Entity> it = originalDeck.iterator();
        int unknownCards = 0;
        while (it.hasNext()) {
            Entity entity = it.next();
            if (TextUtils.isEmpty(entity.extra.tmpCardId)) {
                unknownCards++;
                it.remove();
            }
        }
        Collections.sort(originalDeck, (a,b) -> {
            int r = a.extra.tmpCardId.compareTo(b.extra.tmpCardId);
            return r;
        });

        /**
         * merge all the items with the same tmpCardId
         */
        DeckEntryItem deckEntry = null;
        ArrayList<DeckEntryItem> deckEntryItemList = new ArrayList<>();
        for (Entity entity: originalDeck) {
            if (deckEntry == null || entity.extra.tmpCardId.compareTo(deckEntry.card.id) != 0) {
                if (deckEntry != null) {
                    deckEntryItemList.add(deckEntry);
                }
                deckEntry = new DeckEntryItem();
                deckEntry.card = CardDb.getCard(entity.extra.tmpCardId);
            }
            if (!mOpponent) {
                if (Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    deckEntry.count += 1;
                }
            } else {
                deckEntry.count += 1;
            }
            deckEntry.entityList.add(entity);
        }
        if (deckEntry != null) {
            deckEntryItemList.add(deckEntry);
        }

        /**
         * Add all the gifts on their separate line
         */
        if (mGame != null) {
            String playerId = mOpponent ? mGame.opponent.entity.PlayerID:mGame.player.entity.PlayerID;

            EntityList createdCards = mGame.getEntityList(entity -> {
                return Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))
                        && !playerId.equals(entity.extra.originalController)
                        && playerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))
                        && !TextUtils.isEmpty(entity.CardID);
            });
            for (Entity entity : createdCards) {
                deckEntry = new DeckEntryItem();
                deckEntry.card = entity.card;
                deckEntry.count = 1;
                deckEntry.gift = true;
                deckEntry.entityList.add(entity);
                deckEntryItemList.add(deckEntry);
            }
        }

        for (DeckEntryItem deckEntryItem: deckEntryItemList) {
            if (deckEntryItem.card == null) {
                Timber.e("no card for %s", deckEntryItem.toString());
                deckEntryItem.card = Card.unknown();
            }
        }
        Collections.sort(deckEntryItemList, DeckEntryItem.COMPARATOR);
        for (DeckEntryItem deckEntryItem: deckEntryItemList) {
            Collections.sort(deckEntryItem.entityList, (a,b) -> a.extra.drawTurn - b.extra.drawTurn);
        }

        if (unknownCards > 0) {
            /**
             * I'm not really sure how come this cast is working....
             */
            ArrayList list2 = deckEntryItemList;
            list2.add(ArcaneTrackerApplication.getContext().getString(R.string.unknown_cards, unknownCards));
        }

        return deckEntryItemList;
    }

    private static int compareNullSafe(String a, String b) {
        if (a == null) {
            return b == null ? 0: 1;
        } else {
            return b == null ? -1: a.compareTo(b);
        }
    }
    private ArrayList getHand() {
        ArrayList list = new ArrayList();
        Context context = ArcaneTrackerApplication.getContext();
        String playerId = mGame.opponent.entity.PlayerID;

        EntityList entities = mGame.getEntityList(entity -> {
            return playerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))
                    && Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE));
        });

        Collections.sort(entities, (a, b) -> {
            return compareNullSafe(a.tags.get(Entity.KEY_ZONE_POSITION), b.tags.get(Entity.KEY_ZONE_POSITION));
        });

        HeaderItem headerItem = new HeaderItem();
        headerItem.title = context.getString(R.string.hand) + " (" + entities.size() + ")";
        headerItem.expanded = true;
        list.add(headerItem);
        for (Entity entity : entities) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            if (TextUtils.isEmpty(entity.CardID)) {
                deckEntry.card = Card.unknown();
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(entity.extra.drawTurn);
                if (entity.extra.mulliganed) {
                    builder.append(" (M)");
                }
                deckEntry.card.name = builder.toString();
            } else {
                deckEntry.card = entity.card;
            }
            deckEntry.count = 1;
            deckEntry.entityList.add(entity);
            list.add(deckEntry);
        }

        headerItem = new HeaderItem();
        headerItem.title = context.getString(R.string.played);
        headerItem.expanded = true;
        list.add(headerItem);
        return list;
    }


    protected void update() {
        ArrayList list = new ArrayList();
        if (mOpponent && mGame != null) {
            list.addAll(getHand());
        }
        list.addAll(getDeck());
        mAdapter.setList(list);

    }


    public void resetGame() {
        mGame = null;
        update();
    }

    @Override
    public void gameStarted(Game game) {
        mGame = game;
        update();
    }

    @Override
    public void gameOver() {

    }

    @Override
    public void somethingChanged() {
        update();
    }


    public static Controller getPlayerController() {
        if (sPlayerController == null) {
            sPlayerController = new Controller(false);
        }

        return sPlayerController;
    }

    public static Controller getOpponentController() {
        if (sOpponentController == null) {
            sOpponentController = new Controller(true);
        }

        return sOpponentController;
    }

    public static void resetAll() {
        getOpponentController().resetGame();
        getPlayerController().resetGame();
    }
}
