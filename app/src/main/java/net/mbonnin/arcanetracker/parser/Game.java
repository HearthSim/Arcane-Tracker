package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.CardDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

/**
 */
public class Game {

    private final CardDb cardDb;
    public Map<String, Entity> entityMap = new HashMap<>();
    public List<String> battleTags = new ArrayList<>();

    Map<String,Player> playerMap = new HashMap<>();
    Entity gameEntity;

    public Player player;
    public Player opponent;

    public List<Play> plays = new ArrayList<>();
    public boolean started;

    public Game(CardDb cardDb) {
        this.cardDb = cardDb;
    }

    public Player getPlayer() {
        return player;
    }
    public Player getOpponent() {
        return opponent;
    }

    public Player findController(Entity entity) {
        return findPlayer(entity.tags.get(Entity.KEY_CONTROLLER));
    }

    public Player findPlayer(String playerId) {
        Player player = playerMap.get(playerId);
        if (player == null) {
            Timber.e("cannot find player " + playerId);
            /**
             * do not crash...
             */
            return new Player(cardDb);
        }
        return player;
    }

    public CardDb getCardDb() {
        return cardDb;
    }
}
