package net.mbonnin.arcanetracker.parser;

/**
 * Created by martin on 11/11/16.
 */

import android.text.TextUtils;

import java.util.HashMap;

import timber.log.Timber;

/**
 * takes a FlatGame and turns it into a StructuredGame
 */
public class GameLogic {

    private final Listener mListener;
    HashMap<String,Player> playerMap;
    private Entity gameEntity;

    Player player;
    Player opponent;

    FlatGame flatGame;

    public interface Listener {
        void onGameStarted(GameLogic gameLogic);
        void onGameEnded(GameLogic gameLogic, boolean victory);
    }

    public Player getPlayer() {
        return player;
    }

    public Player getOpponent() {
        return opponent;
    }

    public GameLogic(Listener listener) {
        mListener = listener;
    }

    public void gameStepBeginMulligan() {

        int knownCardsInHand = 0;
        int totalCardsInHand = 0;

        Player player1 = playerMap.get("1");
        Player player2 = playerMap.get("2");

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
        for (String battleTag: flatGame.battleTags) {
            Entity battleTagEntity = flatGame.entityMap.get(battleTag);
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
            flatGame.entityMap.put(battleTag, player.entity);
        }

        player = player1.isOpponent ? player2:player1;
        opponent = player1.isOpponent ? player1:player2;

        mListener.onGameStarted(this);
    }

    public void entityCreated(Entity entity) {
        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
        Player player = findController(entity);

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags.get(Entity.KEY_ZONE));

        if (Entity.CARDTYPE_HERO.equals(cardType)) {
            player.hero = entity;
        } else if (Entity.CARDTYPE_HERO_POWER.equals(cardType)) {
            player.heroPower = entity;
        } else {
            player.entities.add(entity);

            if (gameEntity.tags.get(Entity.KEY_STEP) == null && Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                entity.extra.put(Entity.EXTRA_KEY_ORIGINAL_DECK, Entity.TRUE);
            }
        }

        player.notifyListeners();
    }

    private Player findController(Entity entity) {
        return findPlayer(entity.tags.get(Entity.KEY_CONTROLLER));
    }

    private Player findPlayer(String playerId) {
        Player player = playerMap.get(playerId);
        if (player == null) {
            Timber.e("cannot find player " + playerId);
            /**
             * do not crash...
             */
            return new Player();
        }
        return player;
    }

    public void entityRevealed(Entity entity) {
        Player player = findController(entity);


        player.notifyListeners();
    }

    public void gameStepFinalGameover() {
        boolean victory = Entity.PLAYSTATE_WON.equals(player.entity.tags.get(Entity.KEY_PLAYSTATE));
        mListener.onGameEnded(this, victory);
    }

    public void gameCreated(FlatGame flatGame) {
        this.flatGame = flatGame;
        this.playerMap = new HashMap<>();
        this.player = null;
        this.opponent = null;

        for (Entity entity:flatGame.entityMap.values()) {
            if (entity.PlayerID != null) {
                Timber.i("adding player " + entity.PlayerID);
                Player player = new Player();
                player.entity = entity;
                playerMap.put(entity.PlayerID, player);
            } else if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
                gameEntity = entity;
            }
        }
    }

    public void zoneChanged(Entity entity, String lastZone, String newZone) {
        Player player = findController(entity);
        player.zone(lastZone).remove(entity);
        player.zone(newZone).add(entity);
    }
}
