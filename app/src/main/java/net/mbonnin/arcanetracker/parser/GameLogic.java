package net.mbonnin.arcanetracker.parser;

/**
 * Created by martin on 11/11/16.
 */

import android.os.Handler;
import android.text.TextUtils;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

/**
 * A bunch of helper functions
 */
public class GameLogic {
    private static GameLogic sGameLogic;
    private final Handler mHandler;

    private ArrayList<Listener> mListenerList = new ArrayList<>();
    private Game mGame;
    private int mCurrentTurn;


    public void gameOver() {
        for (Listener listener : mListenerList) {
            listener.gameOver();
        }
    }

    public GameLogic() {
        mHandler = new Handler();
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

        Game game = mGame;
        int knownCardsInHand = 0;
        int totalCardsInHand = 0;

        Player player1 = game.playerMap.get("1");
        Player player2 = game.playerMap.get("2");

        if (player1 == null || player2 == null) {
            Timber.e("cannot find players");
            return;
        }

        EntityList entities = game.getEntityList(entity -> {
            return "1".equals(entity.tags.get(Entity.KEY_CONTROLLER))
                    && Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE));
        });

        for (Entity entity : entities) {
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
        for (String battleTag : game.battleTags) {
            Entity battleTagEntity = game.entityMap.get(battleTag);
            String playsFirst = battleTagEntity.tags.get(Entity.KEY_FIRST_PLAYER);
            Player player;

            if ("1".equals(playsFirst)) {
                player = player1.hasCoin ? player2 : player1;
            } else {
                player = player1.hasCoin ? player1 : player2;
            }

            player.entity.tags.putAll(battleTagEntity.tags);
            player.battleTag = battleTag;

            /**
             * make the battleTag point to the same entity..
             */
            Timber.w(battleTag + " now points to entity " + player.entity.EntityID);
            game.entityMap.put(battleTag, player.entity);
        }

        game.player = player1.isOpponent ? player2 : player1;
        game.opponent = player1.isOpponent ? player1 : player2;
    }

    public void entityCreated(Game game, Entity entity) {
        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
        Player player = game.findController(entity);

        Timber.i("entity created %s controller=%s zone=%s ", entity.EntityID, playerId, entity.tags.get(Entity.KEY_ZONE));

        if (Entity.CARDTYPE_HERO.equals(cardType)) {
            player.hero = entity;
        } else if (Entity.CARDTYPE_HERO_POWER.equals(cardType)) {
            player.heroPower = entity;
        } else {
            if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))){
                entity.extra.drawTurn = (mCurrentTurn + 1) / 2;
            }

            if (game.gameEntity.tags.get(Entity.KEY_STEP) == null) {
                if (Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE))) {
                    entity.extra.originalController = entity.tags.get(Entity.KEY_CONTROLLER);
                } else if (Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))){
                    // this mush be the coin
                    entity.CardID = Card.ID_COIN;
                    entity.extra.drawTurn = 0;
                    entity.card = CardDb.getCard(Card.ID_COIN);
                }
            }
        }

        notifyListeners();
    }

    private Runnable mListenerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mGame != null && mGame.player != null && mGame.opponent != null)
                for (Listener listener : mListenerList) {
                    listener.somethingChanged();
                }
        }
    };

    private void notifyListeners() {
        mHandler.removeCallbacks(mListenerRunnable);
        mHandler.postDelayed(mListenerRunnable, 200);
    }

    public void entityRevealed(Entity entity) {
        Timber.i("entity revealed %s", entity.EntityID);

        notifyListeners();
    }

    public void gameCreated(Game game) {
        mGame = game;
        game.playerMap = new HashMap<>();

        for (Entity entity : game.entityMap.values()) {
            if (entity.PlayerID != null) {
                Timber.i("adding player " + entity.PlayerID);
                Player player = new Player();
                player.entity = entity;
                game.playerMap.put(entity.PlayerID, player);
            } else if (Entity.ENTITY_ID_GAME.equals(entity.EntityID)) {
                game.gameEntity = entity;
            }
        }
    }

    public void tagChanged(Entity entity, String key, String oldValue, String newValue) {
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
            } else if (Entity.ZONE_HAND.equals(oldValue) && Entity.ZONE_DECK.equals(newValue)) {
                /**
                 * card was put back in the deck (most likely from mulligan)
                 */
                entity.extra.drawTurn = -1;
            }
        }

        notifyListeners();
    }

    public void entityPlayed(Entity entity) {
        Game game = mGame;

        if (entity.CardID == null) {
            Timber.e("no CardID for play");
            return;
        }

        Play play = new Play();
        play.turn = mCurrentTurn;
        play.cardId = entity.CardID;
        play.isOpponent = game.findController(entity).isOpponent;

        Timber.i("%s played %s", play.isOpponent ? "opponent" : "I", play.cardId);

        game.plays.add(play);

        notifyListeners();
    }
}
