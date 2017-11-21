package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.CardUtil;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.hsmodel.PlayerClass;
import net.mbonnin.hsmodel.Rarity;
import net.mbonnin.hsmodel.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Controller implements GameLogic.Listener {
    private static Controller sController;

    private final ItemAdapter mPlayerAdapter;
    private final Handler mHandler;
    private final ItemAdapter mOpponentAdapter;
    protected HashMap<String, Integer> mPlayerCardMap;
    private Game mGame;
    private String mPlayerId;
    private String mOpponentId;

    public Controller() {
        mPlayerAdapter = new ItemAdapter();
        mOpponentAdapter = new ItemAdapter();
        GameLogic.get().addListener(this);
        mHandler = new Handler();
    }

    public ItemAdapter getPlayerAdapter() {
        return mPlayerAdapter;
    }

    public ItemAdapter getOpponentAdapter() {
        return mOpponentAdapter;
    }

    public void setPlayerDeck(HashMap<String, Integer> cardMap) {
        mPlayerCardMap = cardMap;
        update();
    }

    private Runnable mUpdateRunnable = this::update;

    private ArrayList<Object> entityListToItemList(EntityList entityList, Function<Entity, Boolean> increasesCount) {
        /*
         * remove and count the really unknown cards
         */
        int unknownCards = 0;
        Iterator<Entity> it = entityList.iterator();
        while (it.hasNext()) {
            Entity entity = it.next();
            if (!entity.extra.tmpIsGift
                    && entity.extra.tmpCard == CardUtil.UNKNOWN) {
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
        for (Map.Entry<String, Integer> entry : mPlayerCardMap.entrySet()) {
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
                    entity.extra.tmpCard = CardUtil.INSTANCE.getCard(cardIdsFromDeck.get(i));
                    i++;
                }
            }
        }
    }

    private EntityList getEntityListInZone(String playerId, String zone) {
        return mGame.getEntityList(entity -> playerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))
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

        EntityList entities = getEntityListInZone(mOpponentId, Entity.ZONE_HAND);

        Collections.sort(entities, (a, b) -> compareNullSafe(a.tags.get(Entity.KEY_ZONE_POSITION), b.tags.get(Entity.KEY_ZONE_POSITION)));

        list.add(new HeaderItem(context.getString(R.string.hand) + " (" + entities.size() + ")"));
        for (Entity entity : entities) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            if (TextUtils.isEmpty(entity.CardID) || entity.extra.hide) {
                deckEntry.card = CardUtil.INSTANCE.unknown();
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn));
                if (entity.extra.mulliganed) {
                    builder.append(" (M)");
                }
                deckEntry.card.name = builder.toString();
            } else {
                deckEntry.card = entity.card;
            }
            deckEntry.gift = !entity.extra.hide && entity.extra.tmpIsGift;
            deckEntry.count = 1;
            Entity clone = entity.clone();
            if (entity.extra.hide) {
                clone.extra.createdBy = null;
            }
            deckEntry.entityList.add(clone);
            list.add(deckEntry);
        }

        return list;
    }


    private void update() {
        if (mGame == null) {
            mPlayerAdapter.setList(getCardMapList(mPlayerCardMap != null ? mPlayerCardMap : new HashMap<>()));
            mOpponentAdapter.setList(getCardMapList(new HashMap<>()));
        } else {
            /*
             * all the code below uses tmpCard and tmpIsGift so that it can change them without messing up the internal game state
             */
            EntityList allEntities = mGame.getEntityList(entity -> true);
            Stream.of(allEntities).forEach(entity -> {
                entity.extra.tmpIsGift = !TextUtils.isEmpty(entity.extra.createdBy);
                if (entity.card != null) {
                    entity.extra.tmpCard = entity.card;
                } else {
                    entity.extra.tmpCard = CardUtil.UNKNOWN;
                }
            });

            updatePlayer();
            updateOpponent();
        }
    }

    private void updateOpponent() {
        ArrayList<Object> list = new ArrayList<>();

        Collection<?> secrets = getSecrets();
        if (secrets.size() > 0) {
            list.add(new HeaderItem(Utils.INSTANCE.getString(R.string.secrets)));
            list.addAll(secrets);
        }
        list.addAll(getHand());

        list.add(new HeaderItem(Utils.INSTANCE.getString(R.string.allCards)));

        // trying a definition that's a bit different from the player definition here
        EntityList allEntities = mGame.getEntityList(e -> mOpponentId.equals(e.tags.get(Entity.KEY_CONTROLLER))
                && !Entity.ZONE_SETASIDE.equals(e.tags.get(Entity.KEY_ZONE))
                && !Type.ENCHANTMENT.equals(e.tags.get(Entity.KEY_CARDTYPE))
                && !Type.HERO.equals(e.tags.get(Entity.KEY_CARDTYPE))
                && !Type.HERO_POWER.equals(e.tags.get(Entity.KEY_CARDTYPE)));

        list.addAll(entityListToItemList(allEntities, e -> true));

        mOpponentAdapter.setList(list);
    }

    private void updatePlayer() {
        ArrayList<Object> list = new ArrayList<>();

        list.add(new HeaderItem(Utils.INSTANCE.getString(R.string.deck)));

        if (mPlayerCardMap != null) {
            assignCardsFromDeck();
        }

        EntityList entityList = mGame.getEntityList(entity -> mPlayerId.equals(entity.extra.originalController));
        /*
         * Add all the gifts
         * XXX it's not enough to filter on !TextUtils.isEmpty(createdBy)
         * because then we get all enchantments
         * if a gift is in the graveyard, it won't be shown but I guess that's ok
         */
        entityList.addAll(mGame.getEntityList(entity -> {
            return Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))
                    && !mPlayerId.equals(entity.extra.originalController)
                    && mPlayerId.equals(entity.tags.get(Entity.KEY_CONTROLLER));
        }));

        list.addAll(entityListToItemList(entityList, entity -> Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))));

        mPlayerAdapter.setList(list);
    }

    private Collection<?> getSecrets() {
        ArrayList<Object> list = new ArrayList<>();

        if (false) {
            Stream.of(getEntityListInZone(mOpponentId, Entity.ZONE_HAND)).forEach(e -> {
                e.tags.put(Entity.KEY_CLASS, PlayerClass.MAGE);
                e.tags.put(Entity.KEY_ZONE, Entity.ZONE_SECRET);
                e.extra.drawTurn = 18;
                e.extra.playTurn = 23;
            });
        }
        EntityList entities = getEntityListInZone(mOpponentId, Entity.ZONE_SECRET)
                .filter(e -> !Rarity.LEGENDARY.equals(e.tags.get(Entity.KEY_RARITY))); // remove quests

        Collections.sort(entities, (a, b) -> compareNullSafe(a.tags.get(Entity.KEY_ZONE_POSITION), b.tags.get(Entity.KEY_ZONE_POSITION)));

        for (Entity entity : entities) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            if (TextUtils.isEmpty(entity.CardID)) {
                String clazz = entity.tags.get(Entity.KEY_CLASS);

                if (clazz != null) {
                    deckEntry.card = CardUtil.INSTANCE.secret(clazz);
                } else {
                    deckEntry.card = CardUtil.INSTANCE.secret("MAGE");
                }
            } else {
                deckEntry.card = entity.card;
            }
            deckEntry.gift = entity.extra.tmpIsGift;
            deckEntry.count = 1;

            Entity clone = entity.clone();
            clone.card = deckEntry.card;
            deckEntry.entityList.add(clone);
            list.add(deckEntry);
        }

        return list;
    }

    private static ArrayList<Object> getCardMapList(HashMap<String, Integer> cardMap) {
        ArrayList<Object> list = new ArrayList<>();
        int unknown = Deck.MAX_CARDS;

        for (Map.Entry<String, Integer> entry : cardMap.entrySet()) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            deckEntry.card = CardUtil.INSTANCE.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            list.add(deckEntry);
            unknown -= deckEntry.count;
        }

        Collections.sort(list, (a, b) -> {
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
        mPlayerId = mGame.player.entity.PlayerID;
        mOpponentId = mGame.opponent.entity.PlayerID;
        update();
    }

    @Override
    public void gameOver() {

    }

    @Override
    public void somethingChanged() {
        /*
         * we gate the notification so as not to flood the listeners
         */
        mHandler.removeCallbacks(mUpdateRunnable);
        mHandler.postDelayed(mUpdateRunnable, 200);
    }


    public static Controller get() {
        if (sController == null) {
            sController = new Controller();
        }

        return sController;
    }

    public static void resetAll() {
        get().resetGame();
    }
}
