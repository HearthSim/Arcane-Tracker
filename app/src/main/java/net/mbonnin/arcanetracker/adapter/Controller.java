package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.text.TextUtils;

import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Controller implements GameLogic.Listener {
    private static Controller sPlayerController;
    private static Controller sOpponentController;

    private final boolean mOpponent;
    private final ItemAdapter mAdapter;
    protected Deck mDeck;
    private Game mGame;
    private String mPlayerId;

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
        EntityList entityList = mGame.getEntityList(entity -> mPlayerId.equals(entity.extra.originalController));

        if (mDeck != null && !mOpponent) {
            assignCardsFromDeck();
        }

        /*
         * Add all the gifts
         */
        entityList.addAll(mGame.getEntityList(entity -> {
            return Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))
                    && !mPlayerId.equals(entity.extra.originalController)
                    && mPlayerId.equals(entity.tags.get(Entity.KEY_CONTROLLER));
        }));

        return entityListToItemList(entityList, entity -> {
            if (mOpponent) {
                return true;
            } else {
                return Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE));
            }
        });
    }

    private ArrayList entityListToItemList(EntityList entityList, Function<Entity, Boolean> increasesCount) {
        /*
         * remove and count the really unknown cards
         */
        int unknownCards = 0;
        Iterator<Entity> it = entityList.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (!entity.extra.tmpIsGift
                    && entity.extra.tmpCard == Card.UNKNOWN) {
                it.remove();
                unknownCards++;
            }
        }

        /*
         * Sort and merge
         */
        Comparator<Entity> comparator = (a, b) -> {
            int acost = a.extra.tmpCard.cost == null ? 0 : a.extra.tmpCard.cost;
            int bcost = b.extra.tmpCard.cost == null ? 0 : b.extra.tmpCard.cost;
            if (acost < 0) {
                acost = Integer.MAX_VALUE;
            }
            if (bcost < 0) {
                bcost = Integer.MAX_VALUE;
            }

            int ret = acost - bcost;

            if (ret != 0) {
                return ret;
            }

            ret = a.extra.tmpCard.name.compareTo(b.extra.tmpCard.name);
            if (ret != 0) {
                return ret;
            }

            int aGift = a.extra.tmpIsGift ? 1 : 0;
            int bGift = b.extra.tmpIsGift ? 1 : 0;

            return bGift - aGift;
        };
        Collections.sort(entityList, comparator);

        Collector<Entity, ArrayList<DeckEntryItem>, ArrayList<DeckEntryItem>> collector = new Collector<Entity, ArrayList<DeckEntryItem>, ArrayList<DeckEntryItem>>() {

            @Override
            public Supplier<ArrayList<DeckEntryItem>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<ArrayList<DeckEntryItem>, Entity> accumulator() {
                return (list, entity) -> {
                    DeckEntryItem deckEntry;
                    if (list.size() == 0 || entity.extra.tmpIsGift) {
                        deckEntry = new DeckEntryItem();
                        list.add(deckEntry);
                    } else {
                        deckEntry = list.get(list.size() - 1);
                        if (entity.extra.tmpCard != deckEntry.card) {
                            deckEntry = new DeckEntryItem();
                            list.add(deckEntry);
                        }
                    }
                    deckEntry.entityList.add(entity);
                    if (increasesCount.apply(entity)) {
                        deckEntry.count++;
                    }
                    deckEntry.card = entity.extra.tmpCard;
                    deckEntry.gift = entity.extra.tmpIsGift;
                };
            }

            @Override
            public Function<ArrayList<DeckEntryItem>, ArrayList<DeckEntryItem>> finisher() {
                return list -> list;
            }
        };

        ArrayList<DeckEntryItem> deckEntryItemList = Stream.of(entityList).collect(collector);

        /*
         * sort the entity list
         */
        for (DeckEntryItem deckEntryItem : deckEntryItemList) {
            Collections.sort(deckEntryItem.entityList, (a, b) -> a.extra.drawTurn - b.extra.drawTurn);
        }

        ArrayList<Object> itemList = new ArrayList<>();
        itemList.addAll(deckEntryItemList);
        if (unknownCards > 0) {
            itemList.add(ArcaneTrackerApplication.getContext().getString(R.string.unknown_cards, unknownCards));
        }

        return itemList;
    }

    /*
     * this attempts to map the knowledge that we have of mDeck to the unknown entities
     * this assumes that an original card is either known or still in deck
     * this sets tmpCard
     *
     * what needs to be handled is hemet, maybe others ?
     */
    private void assignCardsFromDeck() {
        EntityList originalDeckEntityList = mGame.getEntityList(entity -> mPlayerId.equals(entity.extra.originalController));
        ArrayList<String> cardIdsFromDeck = new ArrayList<>();

        /*
         * build a list of all the ids in mDeck
         */
        for (Map.Entry<String, Integer> entry : mDeck.cards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                cardIdsFromDeck.add(entry.getKey());
            }
        }

        /*
         * remove the ones that have been revealed already
         */
        for (Entity entity : originalDeckEntityList) {
            if (!TextUtils.isEmpty(entity.CardID)) {
                Iterator<String> it = cardIdsFromDeck.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    if (next.equals(entity.CardID)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        /*
         * assign a tmpCard to the cards we still don't know
         */
        int i = 0;
        for (Entity entity : originalDeckEntityList) {
            if (entity.card == null) {
                if (i < cardIdsFromDeck.size()) {
                    entity.extra.tmpCard = CardDb.getCard(cardIdsFromDeck.get(i));
                    i++;
                }
            }
        }
    }

    private EntityList getEntityList(String zone) {
        return mGame.getEntityList(entity -> mPlayerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))
                && zone.equals(entity.tags.get(Entity.KEY_ZONE)));
    }

    private static int compareNullSafe(String a, String b) {
        if (a == null) {
            return b == null ? 0 : 1;
        } else {
            return b == null ? -1 : a.compareTo(b);
        }
    }

    private ArrayList getHand() {
        ArrayList<Object> list = new ArrayList<>();
        Context context = ArcaneTrackerApplication.getContext();

        EntityList entities = getEntityList(Entity.ZONE_HAND);

        Collections.sort(entities, (a, b) -> compareNullSafe(a.tags.get(Entity.KEY_ZONE_POSITION), b.tags.get(Entity.KEY_ZONE_POSITION)));

        list.add(new HeaderItem(context.getString(R.string.hand) + " (" + entities.size() + ")"));
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
            deckEntry.gift = entity.extra.tmpIsGift;
            deckEntry.count = 1;
            deckEntry.entityList.add(entity);
            list.add(deckEntry);
        }

        return list;
    }


    protected void update() {
        ArrayList<Object> list = new ArrayList();

        if (mGame != null) {
            /*
             * initialize stuff
             */
            EntityList allEntities = mGame.getEntityList(entity -> true);
            Stream.of(allEntities).forEach(entity -> entity.extra.tmpIsGift = compareNullSafe(entity.tags.get(Entity.KEY_CONTROLLER), entity.extra.originalController) != 0);
            Stream.of(allEntities).forEach(entity -> {
                if (entity.card != null) {
                    entity.extra.tmpCard = entity.card;
                } else {
                    entity.extra.tmpCard = Card.UNKNOWN;
                }
            });
            if (mOpponent) {
                Collection<?> secrets = getSecrets();
                if (secrets.size() > 0) {
                    list.add(new HeaderItem(Utils.getString(R.string.secrets)));
                    list.addAll(secrets);
                }
                list.addAll(getHand());
                list.add(new HeaderItem(Utils.getString(R.string.deck)));
            }
            list.addAll(getDeck());
            //list.addAll(getGraveyard());
        } else {
            if (mDeck != null) {
                list.addAll(getNoGame());
            }
        }
        mAdapter.setList(list);

    }

    private Collection<?> getSecrets() {
        ArrayList<Object> list = new ArrayList<>();

        EntityList entities = getEntityList(Entity.ZONE_SECRET);

        Collections.sort(entities, (a, b) -> compareNullSafe(a.tags.get(Entity.KEY_ZONE_POSITION), b.tags.get(Entity.KEY_ZONE_POSITION)));

        for (Entity entity : entities) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            if (TextUtils.isEmpty(entity.CardID)) {
                String clazz = entity.tags.get(Entity.KEY_CLASS);

                deckEntry.card = Card.unknown();
                if (clazz != null){
                    int classIndex = Card.niceNameToClassIndexNC(clazz);
                    switch (classIndex) {
                        case Card.CLASS_INDEX_HUNTER:
                            deckEntry.card.id = "secret_h";
                            break;
                        case Card.CLASS_INDEX_MAGE:
                            deckEntry.card.id = "secret_m";
                            break;
                        case Card.CLASS_INDEX_PALADIN:
                            deckEntry.card.id = "secret_p";
                            break;
                    }
                }
                deckEntry.card.name = "";
            } else {
                deckEntry.card = entity.card;
            }
            deckEntry.gift = entity.extra.tmpIsGift;
            deckEntry.count = 1;
            deckEntry.entityList.add(entity);
            list.add(deckEntry);
        }

        return list;
    }

    private List<Object> getGraveyard() {
        EntityList entityList = getEntityList(Entity.ZONE_GRAVEYARD);

        ArrayList<Object> list = new ArrayList<>();
        list.add(new HeaderItem(Utils.getString(R.string.graveyard)));

        list.addAll(entityListToItemList(entityList, entity -> true));

        return list;
    }

    private List<Object> getNoGame() {
        List<Object> list = new ArrayList<>();
        int unknown = Deck.MAX_CARDS;

        for (Map.Entry<String, Integer> entry : mDeck.cards.entrySet()) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            deckEntry.card = CardDb.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            list.add(deckEntry);
            unknown -= deckEntry.count;
        }

        Collections.sort(list, (a,b) -> {
            DeckEntryItem da = (DeckEntryItem) a;
            DeckEntryItem db = (DeckEntryItem) b;

            int acost = da.card.cost == null ? 0 : da.card.cost;
            int bcost = db.card.cost == null ? 0 : db.card.cost;
            if (acost < 0) {
                acost = Integer.MAX_VALUE;
            }
            if (bcost < 0) {
                bcost = Integer.MAX_VALUE;
            }
            return acost - bcost;
        });

        if (unknown > 0) {
            list.add(ArcaneTrackerApplication.getContext().getString(R.string.unknown_cards, unknown));
        }
        return list;
    }


    public void resetGame() {
        mGame = null;
        update();
    }

    @Override
    public void gameStarted(Game game) {
        mGame = game;
        mPlayerId = mOpponent ? mGame.opponent.entity.PlayerID : mGame.player.entity.PlayerID;
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
