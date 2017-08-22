package net.mbonnin.arcanetracker.parser;

/*
 * Created by martin on 11/11/16.
 */

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.power.BlockTag;
import net.mbonnin.arcanetracker.parser.power.CreateGameTag;
import net.mbonnin.arcanetracker.parser.power.FullEntityTag;
import net.mbonnin.arcanetracker.parser.power.ShowEntityTag;
import net.mbonnin.arcanetracker.parser.power.Tag;
import net.mbonnin.arcanetracker.parser.power.TagChangeTag;

import java.util.ArrayList;

import timber.log.Timber;

import static net.mbonnin.arcanetracker.parser.power.BlockTag.TYPE_TRIGGER;

public class GameLogic {
    private static GameLogic sGameLogic;

    private ArrayList<Listener> mListenerList = new ArrayList<>();
    private Game mGame;
    private int mCurrentTurn;

    private GameLogic() {
    }

    public void handleRootTag(Tag tag) {
        if (tag instanceof CreateGameTag) {
            handleCreateGameTag((CreateGameTag) tag);
        }

        if (mGame == null) {
            return;
        }

        handleTagRecursive(tag);

        if (!mGame.isStarted()) {
            return;
        }

        handleTagRecursive2(tag);

        notifyListeners();
    }

    private void handleBlockTag(BlockTag tag) {
        for (Tag child : tag.children) {
            if (child instanceof FullEntityTag) {
                tryToGuessCardIdFromBlock(tag, (FullEntityTag) child);
            }
        }
    }

    private void handleBlockTag2(BlockTag tag) {
        Game game = mGame;

        if (BlockTag.TYPE_PLAY.equals(tag.BlockType)) {
            Entity entity = mGame.findEntitySafe(tag.Entity);
            if (entity.CardID == null) {
                Timber.e("no CardID for play");
                return;
            }

            Play play = new Play();
            play.turn = mCurrentTurn;
            play.cardId = entity.CardID;
            play.isOpponent = game.findController(entity).isOpponent;

            Timber.i("%s played %s", play.isOpponent ? "opponent" : "I", play.cardId);

            if (!play.isOpponent) {
                /*
                 * detect if we played a minion or spell for the secret detector
                 */
                try {
                    String opponentPlayerId = mGame.getOpponent().entity.PlayerID;
                    EntityList opponentSecretEntityList = mGame.getEntityList(e -> opponentPlayerId.equals(e.tags.get(Entity.KEY_CONTROLLER)));
                    for (Entity e2 : opponentSecretEntityList) {
                        if (Card.TYPE_MINION.equals(entity.card.type)) {
                            e2.extra.minionPlayed = true;
                        } else if (Card.TYPE_SPELL.equals(entity.card.type)) {
                            e2.extra.spellPlayed = true;
                        } else if (Card.TYPE_HERO_POWER.equals(entity.card.type)) {
                            e2.extra.heroPowerPlayed = true;
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }

            game.plays.add(play);
        }
    }


    private void handleTagRecursive(Tag tag) {
        if (tag instanceof TagChangeTag) {
            handleTagChange((TagChangeTag) tag);
        } else if (tag instanceof FullEntityTag) {
            handleFullEntityTag((FullEntityTag) tag);
        } else if (tag instanceof BlockTag) {
            for (Tag child : ((BlockTag) tag).children) {
                handleTagRecursive(child);
            }
            handleBlockTag((BlockTag) tag);
        } else if (tag instanceof ShowEntityTag) {
            handleShowEntityTag((ShowEntityTag) tag);
        }
    }

    private void handleTagRecursive2(Tag tag) {
        if (tag instanceof TagChangeTag) {
            handleTagChange2((TagChangeTag) tag);
        } else if (tag instanceof FullEntityTag) {
            handleFullEntityTag2((FullEntityTag) tag);
        } else if (tag instanceof BlockTag) {
            for (Tag child : ((BlockTag) tag).children) {
                handleTagRecursive2(child);
            }
            handleBlockTag2((BlockTag) tag);
        } else if (tag instanceof ShowEntityTag) {
            handleShowEntityTag2((ShowEntityTag) tag);
        }
    }

    private void handleShowEntityTag(ShowEntityTag tag) {
        Entity entity = mGame.findEntitySafe(tag.Entity);

        if (!Utils.isEmpty(entity.CardID) && !entity.CardID.equals(tag.CardID)) {
            Timber.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + tag.CardID);
        }
        entity.CardID = tag.CardID;
        entity.card = CardDb.getCard(tag.CardID);


        for (String key : tag.tags.keySet()) {
            tagChanged(entity, key, tag.tags.get(key));
        }
    }


    private void handleShowEntityTag2(ShowEntityTag tag) {
        Entity entity = mGame.findEntitySafe(tag.Entity);

        for (String key : tag.tags.keySet()) {
            tagChanged2(entity, key, tag.tags.get(key));
        }
    }

    private void tagChanged2(Entity entity, String key, String newValue) {
        if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
            if (Entity.KEY_STEP.equals(key) && mGame.isStarted()) {
                if (Entity.STEP_FINAL_GAMEOVER.equals(newValue)) {
                    mGame.victory = Entity.PLAYSTATE_WON.equals(mGame.player.entity.tags.get(Entity.KEY_PLAYSTATE));

                    for (Listener listener : mListenerList) {
                        listener.gameOver();
                    }

                    mGame = null;
                }
            }
        }
    }

    private void tagChanged(Entity entity, String key, String newValue) {
        String oldValue = entity.tags.get(key);

        entity.tags.put(key, newValue);

        if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
            if (Entity.KEY_TURN.equals(key)) {
                try {
                    mCurrentTurn = Integer.parseInt(newValue);
                    Timber.d("turn: " + mCurrentTurn);
                } catch (Exception e) {
                    Timber.e(e);
                }

            }
            if (Entity.KEY_STEP.equals(key)) {
                if (Entity.STEP_BEGIN_MULLIGAN.equals(newValue)) {
                    gameStepBeginMulligan();
                    if (mGame.isStarted()) {
                        for (Listener listener : mListenerList) {
                            listener.gameStarted(mGame);
                        }
                    }
                }
            }
        }

        if (Entity.KEY_ZONE.equals(key)) {
            if (Entity.ZONE_DECK.equals(oldValue) && Entity.ZONE_HAND.equals(newValue)) {
                String step = mGame.gameEntity.tags.get(Entity.KEY_STEP);
                if (step == null) {
                    // this is the original mulligan
                    entity.extra.drawTurn = 0;
                } else if (Entity.STEP_BEGIN_MULLIGAN.equals(step)) {
                    entity.extra.drawTurn = 0;
                    entity.extra.mulliganed = true;
                } else {
                    entity.extra.drawTurn = (mCurrentTurn + 1) / 2;
                }
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_PLAY.equals(newValue)) {
                entity.extra.playTurn = (mCurrentTurn + 1) / 2;
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_SECRET.equals(newValue)) {
                entity.extra.playTurn = (mCurrentTurn + 1) / 2;
            } else if (Entity.ZONE_PLAY.equals(oldValue) && Entity.ZONE_GRAVEYARD.equals(newValue)) {
                entity.extra.diedTurn = (mCurrentTurn + 1) / 2;
//                /*
//                 * one of the oponent minion died, remember it for the secret detector
//                 */
//                try {
//                    String opponentPlayerId = mGame.getOpponent().entity.PlayerID;
//                    EntityList opponentSecretEntityList = mGame.getEntityList(e -> opponentPlayerId.equals(e.tags.get(Entity.KEY_CONTROLLER)));
//                    if (opponentPlayerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))) {
//                        for (Entity e2 : opponentSecretEntityList) {
//                            e2.extra.opponentMinionDied = true;
//                        }
//                    }
//                } catch (Exception e) {
//                    Timber.e(e);
//                }
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_DECK.equals(newValue)) {
                /*
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1;
            }
        }

        notifyListeners();
    }

    private void handleCreateGameTag(CreateGameTag tag) {
        if (mGame != null) {
            Timber.w("CREATE_GAME during an existing one, resuming");
        } else {
            mGame = new Game();

            Player player;
            Entity entity;

            entity = new Entity();
            entity.EntityID = tag.gameEntity.EntityID;
            entity.tags.putAll(tag.gameEntity.tags);
            mGame.addEntity(entity);
            mGame.gameEntity = entity;

            entity = new Entity();
            entity.EntityID = tag.player1.EntityID;
            entity.PlayerID = tag.player1.PlayerID;
            entity.tags.putAll(tag.player1.tags);
            mGame.addEntity(entity);
            player = new Player();
            player.entity = entity;
            mGame.playerMap.put(entity.PlayerID, player);

            entity.EntityID = tag.player2.EntityID;
            entity.PlayerID = tag.player2.PlayerID;
            entity.tags.putAll(tag.player2.tags);
            mGame.addEntity(entity);
            player = new Player();
            player.entity = entity;
            mGame.playerMap.put(entity.PlayerID, player);
        }
    }

    public interface Listener {
        /**
         * when gameStarted is called, game.player and game.opponent are set
         * the initial mulligan cards are known too. It's ok to store 'game' as there can be only one at a time
         */
        void gameStarted(Game game);

        void gameOver();

        /**
         * this is called whenever something changes :)
         */
        void somethingChanged();
    }

    public static GameLogic get() {
        if (sGameLogic == null) {
            sGameLogic = new GameLogic();
        }
        return sGameLogic;
    }

    public void addListener(Listener listener) {
        mListenerList.add(listener);
    }

    private void gameStepBeginMulligan() {

        int knownCardsInHand = 0;
        int totalCardsInHand = 0;

        Player player1 = mGame.playerMap.get("1");
        Player player2 = mGame.playerMap.get("2");

        if (player1 == null || player2 == null) {
            Timber.e("cannot find players");
            return;
        }

        EntityList entities = mGame.getEntityList(entity -> {
            return "1".equals(entity.tags.get(Entity.KEY_CONTROLLER))
                    && Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE));
        });

        for (Entity entity : entities) {
            if (!Utils.isEmpty(entity.CardID)) {
                knownCardsInHand++;
            }
            totalCardsInHand++;
        }

        player1.isOpponent = knownCardsInHand < 3;
        player1.hasCoin = totalCardsInHand > 3;

        player2.isOpponent = !player1.isOpponent;
        player2.hasCoin = !player1.hasCoin;

        /*
         * now try to match a battle tag with a player
         */
        for (String battleTag : mGame.battleTags) {
            Entity battleTagEntity = mGame.entityMap.get(battleTag);
            String playsFirst = battleTagEntity.tags.get(Entity.KEY_FIRST_PLAYER);
            Player player;

            if ("1".equals(playsFirst)) {
                player = player1.hasCoin ? player2 : player1;
            } else {
                player = player1.hasCoin ? player1 : player2;
            }

            player.entity.tags.putAll(battleTagEntity.tags);
            player.battleTag = battleTag;

            /*
             * make the battleTag point to the same entity..
             */
            Timber.w(battleTag + " now points to entity " + player.entity.EntityID);
            mGame.entityMap.put(battleTag, player.entity);
        }

        mGame.player = player1.isOpponent ? player2 : player1;
        mGame.opponent = player1.isOpponent ? player1 : player2;
    }


    private void notifyListeners() {
        if (mGame != null && mGame.isStarted()) {
            for (Listener listener : mListenerList) {
                listener.somethingChanged();
            }
        }
    }

    private void handleTagChange(TagChangeTag tag) {
        tagChanged(mGame.findEntitySafe(tag.ID), tag.tag, tag.value);
    }

    private void handleTagChange2(TagChangeTag tag) {
        tagChanged2(mGame.findEntitySafe(tag.ID), tag.tag, tag.value);
    }

    private void tryToGuessCardIdFromBlock(BlockTag blockTag, FullEntityTag fullEntityTag) {
        Entity blockEntity = mGame.findEntitySafe(blockTag.Entity);
        Entity entity = mGame.findEntitySafe(fullEntityTag.ID);

        String actionStartingCardId = blockEntity.CardID;

        if (Utils.isEmpty(actionStartingCardId)) {
            return;
        }

        String guessedId = null;

        if (BlockTag.TYPE_POWER.equals(blockTag.BlockType)) {

            switch (actionStartingCardId) {
                case Card.ID_GANG_UP:
                case Card.ID_RECYCLE:
                case Card.SHADOWCASTER:
                case Card.MANIC_SOULCASTER:
                    guessedId = mGame.findEntitySafe(blockTag.Target).CardID;
                    break;
                case Card.ID_BENEATH_THE_GROUNDS:
                    guessedId = Card.ID_AMBUSHTOKEN;
                    break;
                case Card.ID_IRON_JUGGERNAUT:
                    guessedId = Card.ID_BURROWING_MINE_TOKEN;
                    break;
                case Card.FORGOTTEN_TORCH:
                    guessedId = Card.ROARING_TORCH;
                    break;
                case Card.CURSE_OF_RAFAAM:
                    guessedId = Card.CURSED;
                    break;
                case Card.ANCIENT_SHADE:
                    guessedId = Card.ANCIENT_CURSE;
                    break;
                case Card.EXCAVATED_EVIL:
                    guessedId = Card.EXCAVATED_EVIL;
                    break;
                case Card.ELISE:
                    guessedId = Card.MAP_TO_THE_GOLDEN_MONKEY;
                    break;
                case Card.MAP_TO_THE_GOLDEN_MONKEY:
                    guessedId = Card.GOLDEN_MONKEY;
                    break;
                case Card.DOOMCALLER:
                    guessedId = Card.CTHUN;
                    break;
                case Card.JADE_IDOL:
                    guessedId = Card.JADE_IDOL;
                    break;
                case Card.FLAME_GEYSER:
                case Card.FIREFLY:
                    guessedId = Card.FLAME_ELEMENTAL;
                    break;
                case Card.STEAM_SURGER:
                    guessedId = Card.FLAME_GEYSER;
                    break;
                case Card.RAZORPETAL_VOLLEY:
                case Card.RAZORPETAL_LASHER:
                    guessedId = Card.RAZORPETAL;
                    break;
                case Card.BURGLY_BULLY:
                    guessedId = Card.ID_COIN;
                    break;
                case Card.MUKLA_TYRANT:
                case Card.KING_MUKLA:
                    guessedId = Card.BANANA;
                    break;
                case Card.JUNGLE_GIANTS:
                    guessedId = Card.BARNABUS;
                    break;
                case Card.THE_MARSH_QUEEN:
                    guessedId = Card.QUEEN_CARNASSA;
                    break;
                case Card.OPEN_THE_WAYGATE:
                    guessedId = Card.TIME_WARP;
                    break;
                case Card.THE_LAST_KALEIDOSAUR:
                    guessedId = Card.GALVADON;
                    break;
                case Card.AWAKEN_THE_MAKERS:
                    guessedId = Card.AMARA;
                    break;
                case Card.CAVERNS_BELOW:
                    guessedId = Card.CRYSTAL_CORE;
                    break;
                case Card.UNITE_THE_MURLOCS:
                    guessedId = Card.MEGAFIN;
                    break;
                case Card.LAKKARI_SACRIFICE:
                    guessedId = Card.NETHER_PORTAL;
                    break;
                case Card.FIRE_PLUME:
                    guessedId = Card.SULFURAS;
                    break;
            }
        } else if (TYPE_TRIGGER.equals(blockTag.BlockType)) {
            switch (actionStartingCardId) {
                case Card.PYROS2:
                    guessedId = Card.PYROS6;
                    break;
                case Card.PYROS6:
                    guessedId = Card.PYROS10;
                    break;
                case Card.WHITE_EYES:
                    guessedId = Card.STORM_GUARDIAN;
                    break;
                case Card.DEADLY_FORK:
                    guessedId = Card.SHARP_FORK;
                    break;
                case Card.IGNEOUS_ELEMENTAL:
                    guessedId = Card.FLAME_ELEMENTAL;
                    break;
                case Card.RHONIN:
                    guessedId = Card.ARCANE_MISSILE;
                    break;
            }
        }
        if (guessedId != null) {
            entity.CardID = guessedId;
            entity.card = CardDb.getCard(guessedId);
            entity.extra.createdBy = guessedId;
        }
    }

    private void handleFullEntityTag2(FullEntityTag tag) {

    }

    private void handleFullEntityTag(FullEntityTag tag) {
        Entity entity = mGame.entityMap.get(tag.ID);

        if (entity == null) {
            entity = new Entity();
            mGame.entityMap.put(tag.ID, entity);
        }
        entity.EntityID = tag.ID;
        entity.CardID = tag.CardID;
        if (!Utils.isEmpty(entity.CardID)) {
            entity.card = CardDb.getCard(entity.CardID);
        }
        entity.tags.putAll(tag.tags);

        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
        Player player = mGame.findController(entity);

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags.get(Entity.KEY_ZONE));

        if (Entity.CARDTYPE_HERO.equals(cardType)) {
            player.hero = entity;
        } else if (Entity.CARDTYPE_HERO_POWER.equals(cardType)) {
            player.heroPower = entity;
        } else {
            if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))) {
                entity.extra.drawTurn = (mCurrentTurn + 1) / 2;
            }

            if (mGame.gameEntity.tags.get(Entity.KEY_STEP) == null) {
                if (Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    entity.extra.originalController = entity.tags.get(Entity.KEY_CONTROLLER);
                } else if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    // this mush be the coin
                    entity.CardID = Card.ID_COIN;
                    entity.extra.drawTurn = 0;
                    entity.card = CardDb.getCard(Card.ID_COIN);
                }
            }
        }

        notifyListeners();
    }
}
