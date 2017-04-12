package net.mbonnin.arcanetracker.parser;

/**
 * Created by martin on 11/11/16.
 */

import android.text.TextUtils;

import java.util.HashMap;

import timber.log.Timber;

/**
 * A bunch of helper functions
 */
public class GameLogic {

    private static void gameStepBeginMulligan(Game game) {

        int knownCardsInHand = 0;
        int totalCardsInHand = 0;

        Player player1 = game.playerMap.get("1");
        Player player2 = game.playerMap.get("2");

        if (player1 == null || player2 == null) {
            Timber.e("cannot find players");
            return;
        }

        for (Entity entity: player1.zone(Entity.ZONE_HAND)) {
            if (!TextUtils.isEmpty(entity.CardID)) {
                knownCardsInHand++;
            }
            totalCardsInHand++;
        }

        player1.isOpponent = knownCardsInHand < 3;
        player1.hasCoin = totalCardsInHand > 3;

        player2.isOpponent = !player1.isOpponent;
        player2.hasCoin = !player1.hasCoin;

        /**
         * now try to match a battle tag with a player
         */
        for (String battleTag: game.battleTags) {
            Entity battleTagEntity = game.entityMap.get(battleTag);
            String playsFirst = battleTagEntity.tags.get(Entity.KEY_FIRST_PLAYER);
            Player player;

            if ("1".equals(playsFirst)) {
                player = player1.hasCoin ? player2: player1;
            } else {
                player = player1.hasCoin ? player1: player2;
            }

            player.entity.tags.putAll(battleTagEntity.tags);
            player.battleTag = battleTag;

            /**
             * make the battleTag point to the same entity..
             */
            Timber.w(battleTag + " now points to entity " + player.entity.EntityID);
            game.entityMap.put(battleTag, player.entity);
        }

        game.player = player1.isOpponent ? player2:player1;
        game.opponent = player1.isOpponent ? player1:player2;
    }


    private static void zoneChanged(Game game, Entity entity, String lastZone, String newZone) {
        Player player = game.findController(entity);
        player.zone(lastZone).remove(entity);
        player.zone(newZone).add(entity);
    }

    public static void entityCreated(Game game, Entity entity) {
        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
        Player player = game.findController(entity);

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags.get(Entity.KEY_ZONE));

        if (Entity.CARDTYPE_HERO.equals(cardType)) {
            player.hero = entity;
        } else if (Entity.CARDTYPE_HERO_POWER.equals(cardType)) {
            player.heroPower = entity;
        } else {
            player.entities.add(entity);

            if (game.gameEntity.tags.get(Entity.KEY_STEP) == null && Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                entity.extra.put(Entity.EXTRA_KEY_ORIGINAL_DECK, Entity.TRUE);
            }
        }

        player.notifyListeners();
    }

    public static void entityRevealed(Game game, Entity entity) {
        Player player = game.findController(entity);

        player.notifyListeners();
    }

    public static void gameCreated(Game game) {
        game.playerMap = new HashMap<>();

        for (Entity entity: game.entityMap.values()) {
            if (entity.PlayerID != null) {
                Timber.i("adding player " + entity.PlayerID);
                Player player = new Player(game.getCardDb());
                player.entity = entity;
                game.playerMap.put(entity.PlayerID, player);
            } else if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
                game.gameEntity = entity;
            }
        }
    }

    public static void tagChanged(Game game, Entity entity, String key, String oldValue, String newValue) {
        if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
            if (Entity.KEY_STEP.equals(key)) {
                if (Entity.STEP_BEGIN_MULLIGAN.equals(newValue)) {
                    GameLogic.gameStepBeginMulligan(game);
                }
            }
        } else {
            if (Entity.KEY_ZONE.equals(key)) {
                if (!TextUtils.isEmpty(oldValue) && !oldValue.equals(newValue)) {
                    GameLogic.zoneChanged(game, entity, oldValue, newValue);
                }
            }
        }
    }

    public static void entityPlayed(Game game, Entity entity) {
        String turn = game.gameEntity.tags.get(Entity.KEY_TURN);
        if (turn == null) {
            Timber.e("cannot get turn");
            return;
        }
        if (entity.CardID == null) {
            Timber.e("no CardID for play");
            return;
        }

        Play play = new Play();
        play.turn = Integer.parseInt(turn);
        play.cardId = entity.CardID;
        play.isOpponent = game.findController(entity).isOpponent;

        Timber.i("%s played %s", play.isOpponent ? "opponent":"I", play.cardId);

        game.plays.add(play);
    }
}
