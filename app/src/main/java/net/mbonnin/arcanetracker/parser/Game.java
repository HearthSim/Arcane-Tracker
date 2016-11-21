package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

/**
 */
public class Game {

    public HashMap<String, Entity> entityMap = new HashMap<>();
    public ArrayList<String> battleTags = new ArrayList<>();

    HashMap<String,Player> playerMap = new HashMap<>();
    Entity gameEntity;

    public Player player;
    public Player opponent;

    public ArrayList<Play> plays = new ArrayList<>();
    public boolean started;

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
            return new Player();
        }
        return player;
    }
}
