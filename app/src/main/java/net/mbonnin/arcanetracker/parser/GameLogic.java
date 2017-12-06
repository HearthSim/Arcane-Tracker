package net.mbonnin.arcanetracker.parser;

/*
 * Created by martin on 11/11/16.
 */

import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.power.BlockTag;
import net.mbonnin.arcanetracker.parser.power.CreateGameTag;
import net.mbonnin.arcanetracker.parser.power.FullEntityTag;
import net.mbonnin.arcanetracker.parser.power.MetaDataTag;
import net.mbonnin.arcanetracker.parser.power.PlayerTag;
import net.mbonnin.arcanetracker.parser.power.ShowEntityTag;
import net.mbonnin.arcanetracker.parser.power.Tag;
import net.mbonnin.arcanetracker.parser.power.TagChangeTag;
import net.mbonnin.hsmodel.CardId;
import net.mbonnin.hsmodel.Rarity;
import net.mbonnin.hsmodel.Type;

import java.util.ArrayList;

import timber.log.Timber;

import static net.mbonnin.arcanetracker.parser.power.BlockTag.TYPE_TRIGGER;

public class GameLogic {
    private static GameLogic sGameLogic;

    private ArrayList<Listener> mListenerList = new ArrayList<>();
    private Game mGame;
    private int mCurrentTurn;
    private boolean mLastTag;

    private GameLogic() {
    }

    public void handleRootTag(Tag tag) {
        //Timber.d("handle tag: " + tag);
        if (tag instanceof CreateGameTag) {
            handleCreateGameTag((CreateGameTag) tag);
        }

        if (mGame != null) {
            handleTagRecursive(tag);
            if (mGame.isStarted()) {
                handleTagRecursive2(tag);

                guessIds(tag);

                notifyListeners();
            }

            if (mLastTag) {
                if (mGame.isStarted()) {
                    mGame.victory = Entity.PLAYSTATE_WON.equals(mGame.player.entity.tags.get(Entity.KEY_PLAYSTATE));
                    for (Listener listener : mListenerList) {
                        listener.gameOver();
                    }
                }

                mGame = null;
                mLastTag = false;
            }
        }
    }

    private void guessIds(Tag tag) {
        ArrayList<BlockTag> stack = new ArrayList<>();

        guessIdsRecursive(stack, tag);
    }

    private void guessIdsRecursive(ArrayList<BlockTag> stack, Tag tag) {
        if (tag instanceof FullEntityTag) {
            tryToGuessCardIdFromBlock(stack, (FullEntityTag) tag);
        } else if (tag instanceof BlockTag) {
            stack.add((BlockTag) tag);
            for (Tag child : ((BlockTag) tag).children) {
                guessIdsRecursive(stack, child);
            }
        }
    }

    private void handleBlockTag(BlockTag tag) {
    }

    private void handleBlockTag2(BlockTag tag) {
        Game game = mGame;

        if (BlockTag.TYPE_PLAY.equals(tag.BlockType)) {
            Entity playedEntity = mGame.findEntitySafe(tag.Entity);
            if (playedEntity.CardID == null) {
                Timber.e("no CardID for play");
                return;
            }

            Play play = new Play();
            play.turn = mCurrentTurn;
            play.cardId = playedEntity.CardID;
            play.isOpponent = game.findController(playedEntity).isOpponent;

            mGame.lastPlayedCardId = play.cardId;
            Timber.i("%s played %s", play.isOpponent ? "opponent" : "I", play.cardId);

            /*
             * secret detector
             */
            EntityList secretEntityList = getSecretEntityList();
            for (Entity secretEntity : secretEntityList) {
                if (!Utils.INSTANCE.equalsNullSafe(secretEntity.tags.get(Entity.KEY_CONTROLLER), playedEntity.tags.get(Entity.KEY_CONTROLLER))
                        && !Utils.INSTANCE.isEmpty(playedEntity.CardID)) {
                    /*
                     * it can happen that we don't know the id of the played entity, for an example if the player has a secret and its opponent plays one
                     * it should be ok to ignore those since these are opponent plays
                     */
                    if (Type.MINION.equals(playedEntity.tags.get(Entity.KEY_CARDTYPE))) {
                        secretEntity.extra.otherPlayerPlayedMinion = true;
                        if (getMinionsOnBoardForController(playedEntity.tags.get(Entity.KEY_CONTROLLER)).size() >= 3) {
                            secretEntity.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready = true;
                        }
                    } else if (Type.SPELL.equals(playedEntity.tags.get(Entity.KEY_CARDTYPE))) {
                        secretEntity.extra.otherPlayerCastSpell = true;
                        Entity targetEntiy = mGame.findEntityUnsafe(tag.Target);
                        if (targetEntiy != null && Type.MINION.equals(targetEntiy.tags.get(Entity.KEY_CARDTYPE))) {
                            secretEntity.extra.selfMinionTargetedBySpell = true;
                        }
                    } else if (Type.HERO_POWER.equals(playedEntity.tags.get(Entity.KEY_CARDTYPE))) {
                        secretEntity.extra.otherPlayerHeroPowered = true;
                    }
                }
            }

            game.plays.add(play);
        } else if (BlockTag.TYPE_ATTACK.equals(tag.BlockType)) {
            /*
             * secret detector
             */
            Entity targetEntity = mGame.findEntitySafe(tag.Target);

            EntityList secretEntityList = getSecretEntityList();
            for (Entity secretEntity : secretEntityList) {
                if (Utils.INSTANCE.equalsNullSafe(secretEntity.tags.get(Entity.KEY_CONTROLLER), targetEntity.tags.get(Entity.KEY_CONTROLLER))) {
                    if (Type.MINION.equals(targetEntity.tags.get(Entity.KEY_CARDTYPE))) {
                        secretEntity.extra.selfMinionWasAttacked = true;
                    } else if (Type.HERO.equals(targetEntity.tags.get(Entity.KEY_CARDTYPE))) {
                        secretEntity.extra.selfHeroAttacked = true;
                        Entity attackerEntity = mGame.findEntitySafe(tag.Entity);
                        if (Type.MINION.equals(attackerEntity.tags.get(Entity.KEY_CARDTYPE))) {
                            secretEntity.extra.selfHeroAttackedByMinion = true;
                        }
                    }
                }
            }

        }
    }


    private void handleTagRecursive(Tag tag) {
        if (tag instanceof TagChangeTag) {
            handleTagChange((TagChangeTag) tag);
        } else if (tag instanceof FullEntityTag) {
            handleFullEntityTag((FullEntityTag) tag);
        } else if (tag instanceof BlockTag) {
            handleBlockTag((BlockTag) tag);
            for (Tag child : ((BlockTag) tag).children) {
                handleTagRecursive(child);
            }
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
            handleBlockTag2((BlockTag) tag);
            for (Tag child : ((BlockTag) tag).children) {
                handleTagRecursive2(child);
            }
        } else if (tag instanceof ShowEntityTag) {
            handleShowEntityTag2((ShowEntityTag) tag);
        } else if (tag instanceof MetaDataTag) {
            handleMetaDataTag2((MetaDataTag) tag);
        }
    }

    private void handleMetaDataTag2(MetaDataTag tag) {
        if (MetaDataTag.META_DAMAGE.equals(tag.Meta)) {
            /*
             * secret detector
             */
            try {
                int damage = Integer.parseInt(tag.Data);
                if (damage > 0) {
                    for (String id : tag.Info) {
                        Entity damagedEntity = mGame.findEntitySafe(id);
                        EntityList secretEntityList = getSecretEntityList();
                        for (Entity e2 : secretEntityList) {
                            if (Utils.INSTANCE.equalsNullSafe(e2.tags.get(Entity.KEY_CONTROLLER), damagedEntity.tags.get(Entity.KEY_CONTROLLER))) {
                                if (Type.HERO.equals(damagedEntity.tags.get(Entity.KEY_CARDTYPE))) {
                                    e2.extra.selfHeroDamaged = true;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private void handleShowEntityTag(ShowEntityTag tag) {
        Entity entity = mGame.findEntitySafe(tag.Entity);

        if (!Utils.INSTANCE.isEmpty(entity.CardID) && !entity.CardID.equals(tag.CardID)) {
            Timber.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + tag.CardID);
        }
        entity.setCardId(tag.CardID);

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

            } else if (Entity.KEY_STEP.equals(key)) {
                if (Entity.STEP_BEGIN_MULLIGAN.equals(newValue)) {
                    gameStepBeginMulligan();
                    if (mGame.isStarted()) {
                        for (Listener listener : mListenerList) {
                            listener.gameStarted(mGame);
                        }
                    }
                } else if (Entity.STEP_FINAL_GAMEOVER.equals(newValue)) {
                    // do not set mGame = null here, we might be part of a block where other tag handlers
                    // require access to mGame
                    mLastTag = true;
                }
            }
        }

        if (Entity.KEY_ZONE.equals(key)) {
            if (!Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_HAND.equals(newValue)) {
                String step = mGame.gameEntity.tags.get(Entity.KEY_STEP);
                if (step == null) {
                    // this is the original mulligan
                    entity.extra.drawTurn = 0;
                } else if (Entity.STEP_BEGIN_MULLIGAN.equals(step)) {
                    entity.extra.drawTurn = 0;
                    entity.extra.mulliganed = true;
                } else {
                    entity.extra.drawTurn = mCurrentTurn;
                }

                if (Entity.ZONE_DECK.equals(oldValue)) {
                    // we should not give too much information about what cards the opponent has
                    entity.extra.hide = true;
                }
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_PLAY.equals(newValue)) {
                entity.extra.playTurn = mCurrentTurn;
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_SECRET.equals(newValue)) {
                entity.extra.playTurn = mCurrentTurn;
            } else if (Entity.ZONE_PLAY.equals(oldValue) && Entity.ZONE_GRAVEYARD.equals(newValue)) {
                entity.extra.diedTurn = mCurrentTurn;
                /*
                 * secret detector
                 */
                EntityList secretEntityList = mGame.getEntityList(e -> Entity.ZONE_SECRET.equals(e.tags.get(Entity.KEY_ZONE)));
                for (Entity secretEntity : secretEntityList) {
                    if (Utils.INSTANCE.equalsNullSafe(secretEntity.tags.get(Entity.KEY_CONTROLLER), entity.tags.get(Entity.KEY_CONTROLLER))
                            && Type.MINION.equals(entity.tags.get(Entity.KEY_CARDTYPE))) {
                            Entity controllerEntity = mGame.findControllerEntity(entity);
                            if (controllerEntity != null && "0".equals(controllerEntity.tags.get(Entity.KEY_CURRENT_PLAYER))) {
                                secretEntity.extra.selfPlayerMinionDied = true;
                            }
                    }
                }
            } else if (Entity.ZONE_HAND.equals(oldValue) && !Entity.ZONE_HAND.equals(newValue)) {
                /*
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1;
                /*
                 * no reason to hide it anymore
                 */
                entity.extra.hide = false;
            }
        }

        if (Entity.KEY_TURN.equals(key)) {
            /*
             * secret detector
             */
            EntityList secretEntityList = getSecretEntityList();
            Entity currentPlayer = null;
            for (Player player : mGame.playerMap.values()) {
                if ("1".equals(player.entity.tags.get(Entity.KEY_CURRENT_PLAYER))) {
                    currentPlayer = player.entity;
                    Timber.d("Current player: " + currentPlayer.PlayerID + "(" + player.battleTag + ")");
                    break;
                }
            }
            for (Entity secretEntity : secretEntityList) {
                if (currentPlayer != null && Utils.INSTANCE.equalsNullSafe(secretEntity.tags.get(Entity.KEY_CONTROLLER), currentPlayer.PlayerID)) {
                    EntityList list = getMinionsOnBoardForController(secretEntity.tags.get(Entity.KEY_CONTROLLER));
                    if (!list.isEmpty()) {
                        Timber.d("Competitive condition");
                        secretEntity.extra.competitiveSpiritTriggerConditionHappened = true;
                    }
                }
            }
        }
    }

    private EntityList getSecretEntityList() {
        /*
         * don't factor in the epic secret which are all quests for now
         */
        return mGame.getEntityList(e -> Entity.ZONE_SECRET.equals(e.tags.get(Entity.KEY_ZONE)))
                .filter(e -> !Rarity.LEGENDARY.equals(e.tags.get(Entity.KEY_RARITY)));
    }

    private EntityList getMinionsOnBoardForController(String playerId) {
        return mGame.getEntityList(e -> {
            if (!Entity.ZONE_PLAY.equals(e.tags.get(Entity.KEY_ZONE))) {
                return false;
            }
            if (!Type.MINION.equals(e.tags.get(Entity.KEY_CARDTYPE))) {
                return false;
            }
            return Utils.INSTANCE.equalsNullSafe(playerId, e.tags.get(Entity.KEY_CONTROLLER));
        });
    }

    private void handleCreateGameTag(CreateGameTag tag) {
        mLastTag = false;

        if (mGame != null && tag.gameEntity.tags.get(Entity.KEY_TURN) != null) {
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

            for (PlayerTag playerTag : tag.playerList) {
                entity = new Entity();
                entity.EntityID = playerTag.EntityID;
                entity.PlayerID = playerTag.PlayerID;
                entity.tags.putAll(playerTag.tags);
                mGame.addEntity(entity);
                player = new Player();
                player.entity = entity;
                mGame.playerMap.put(entity.PlayerID, player);
            }
        }
    }

    public void removeListener(Listener listener) {
        mListenerList.remove(listener);
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
            if (!Utils.INSTANCE.isEmpty(entity.CardID)) {
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

    private void tryToGuessCardIdFromBlock(ArrayList<BlockTag> stack, FullEntityTag fullEntityTag) {
        if (stack.isEmpty()) {
            return;
        }

        BlockTag blockTag = stack.get(stack.size() - 1);

        Entity blockEntity = mGame.findEntitySafe(blockTag.Entity);
        Entity entity = mGame.findEntitySafe(fullEntityTag.ID);

        if (Utils.INSTANCE.isEmpty(blockEntity.CardID)) {
            return;
        }

        String guessedId = null;

        if (BlockTag.TYPE_POWER.equals(blockTag.BlockType)) {

            switch (blockEntity.CardID) {
                case CardId.GANG_UP:
                case CardId.RECYCLE:
                case CardId.SHADOWCASTER:
                case CardId.MANIC_SOULCASTER:
                    guessedId = mGame.findEntitySafe(blockTag.Target).CardID;
                    break;
                case CardId.BENEATH_THE_GROUNDS:
                    guessedId = CardId.NERUBIAN_AMBUSH;
                    break;
                case CardId.IRON_JUGGERNAUT:
                    guessedId = CardId.BURROWING_MINE;
                    break;
                case CardId.FORGOTTEN_TORCH:
                    guessedId = CardId.ROARING_TORCH;
                    break;
                case CardId.CURSE_OF_RAFAAM:
                    guessedId = CardId.CURSED;
                    break;
                case CardId.ANCIENT_SHADE:
                    guessedId = CardId.ANCIENT_CURSE;
                    break;
                case CardId.EXCAVATED_EVIL:
                    guessedId = CardId.EXCAVATED_EVIL;
                    break;
                case CardId.ELISE_STARSEEKER:
                    guessedId = CardId.MAP_TO_THE_GOLDEN_MONKEY;
                    break;
                case CardId.MAP_TO_THE_GOLDEN_MONKEY:
                    guessedId = CardId.GOLDEN_MONKEY;
                    break;
                case CardId.DOOMCALLER:
                    guessedId = CardId.CTHUN;
                    break;
                case CardId.JADE_IDOL:
                    guessedId = CardId.JADE_IDOL;
                    break;
                case CardId.FLAME_GEYSER:
                case CardId.FIRE_FLY:
                    guessedId = CardId.FLAME_ELEMENTAL;
                    break;
                case CardId.STEAM_SURGER:
                    guessedId = CardId.FLAME_GEYSER;
                    break;
                case CardId.RAZORPETAL_VOLLEY:
                case CardId.RAZORPETAL_LASHER:
                    guessedId = CardId.RAZORPETAL;
                    break;
                case CardId.MUKLA_TYRANT_OF_THE_VALE:
                case CardId.KING_MUKLA:
                    guessedId = CardId.BANANAS;
                    break;
                case CardId.JUNGLE_GIANTS:
                    guessedId = CardId.BARNABUS_THE_STOMPER;
                    break;
                case CardId.THE_MARSH_QUEEN:
                    guessedId = CardId.QUEEN_CARNASSA;
                    break;
                case CardId.OPEN_THE_WAYGATE:
                    guessedId = CardId.TIME_WARP;
                    break;
                case CardId.THE_LAST_KALEIDOSAUR:
                    guessedId = CardId.GALVADON;
                    break;
                case CardId.AWAKEN_THE_MAKERS:
                    guessedId = CardId.AMARA_WARDEN_OF_HOPE;
                    break;
                case CardId.THE_CAVERNS_BELOW:
                    guessedId = CardId.CRYSTAL_CORE;
                    break;
                case CardId.UNITE_THE_MURLOCS:
                    guessedId = CardId.MEGAFIN;
                    break;
                case CardId.LAKKARI_SACRIFICE:
                    guessedId = CardId.NETHER_PORTAL;
                    break;
                case CardId.FIRE_PLUMES_HEART:
                    guessedId = CardId.SULFURAS;
                    break;
                case CardId.GHASTLY_CONJURER:
                    guessedId = CardId.MIRROR_IMAGE;
                    break;
                case CardId.EXPLORE_UNGORO:
                    guessedId = CardId.CHOOSE_YOUR_PATH;
                    break;
                case CardId.ELISE_THE_TRAILBLAZER:
                    guessedId = CardId.UNGORO_PACK;
                    break;
            }
        } else if (TYPE_TRIGGER.equals(blockTag.BlockType)) {
            switch (blockEntity.CardID) {
                case CardId.PYROS:
                    guessedId = CardId.PYROS1;
                    break;
                case CardId.PYROS1:
                    guessedId = CardId.PYROS2;
                    break;
                case CardId.WHITE_EYES:
                    guessedId = CardId.THE_STORM_GUARDIAN;
                    break;
                case CardId.DEADLY_FORK:
                    guessedId = CardId.SHARP_FORK;
                    break;
                case CardId.BURGLY_BULLY:
                    guessedId = CardId.THE_COIN;
                    break;
                case CardId.IGNEOUS_ELEMENTAL:
                    guessedId = CardId.FLAME_ELEMENTAL;
                    break;
                case CardId.RHONIN:
                    guessedId = CardId.ARCANE_MISSILES;
                    break;
                case CardId.FROZEN_CLONE:
                    for (BlockTag parent : stack) {
                        if (BlockTag.TYPE_PLAY.equals(parent.BlockType)) {
                            guessedId = mGame.findEntitySafe(parent.Entity).CardID;
                            break;
                        }
                    }
                    break;
                case CardId.BONE_BARON:
                    guessedId = CardId.SKELETON;
                    break;
                case CardId.WEASEL_TUNNELER:
                    guessedId = CardId.WEASEL_TUNNELER;
                    break;
                case CardId.RAPTOR_HATCHLING:
                    guessedId = CardId.RAPTOR_PATRIARCH;
                    break;
                case CardId.DIREHORN_HATCHLING:
                    guessedId = CardId.DIREHORN_MATRIARCH;
                    break;
                case CardId.MANA_BIND:
                    for (BlockTag parent : stack) {
                        if (BlockTag.TYPE_PLAY.equals(parent.BlockType)) {
                            guessedId = mGame.findEntitySafe(parent.Entity).CardID;
                            break;
                        }
                    }
                    break;
                case CardId.ARCHMAGE_ANTONIDAS:
                    guessedId = CardId.FIREBALL;
                    break;

            }
        }
        if (!Utils.INSTANCE.isEmpty(guessedId)) {
            entity.setCardId(guessedId);
        }

        // even if we don't know the guessedId, record that this was createdBy this entity
        entity.extra.createdBy = blockEntity.CardID;
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
        if (!Utils.INSTANCE.isEmpty(tag.CardID)) {
            entity.setCardId(tag.CardID);
        }
        entity.tags.putAll(tag.tags);

        if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))) {
            entity.extra.drawTurn = mCurrentTurn;
        }

        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
        Player player = mGame.findController(entity);

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags.get(Entity.KEY_ZONE));

        if (Type.HERO.equals(cardType)) {
            player.hero = entity;
        } else if (Type.HERO_POWER.equals(cardType)) {
            player.heroPower = entity;
        } else {
            if (mGame.gameEntity.tags.get(Entity.KEY_STEP) == null) {
                if (Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    entity.extra.originalController = entity.tags.get(Entity.KEY_CONTROLLER);
                } else if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    // this must be the coin
                    entity.setCardId(CardId.THE_COIN);
                    entity.extra.drawTurn = 0;
                }
            }
        }
    }

    public static int gameTurnToHumanTurn(int turn) {
        return (turn + 1) / 2;
    }
}
