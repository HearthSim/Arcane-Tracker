package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class Game {
    private HashMap<String, Player> playerMap = new HashMap<>();
    private HashMap<String, Entity> entityMap = new HashMap<>();

    public GameEntity entity;

    public ArrayList<String> battleTags = new ArrayList<>();

    public boolean victory;

    public ArrayList<ArrayList<Action>> turns = new ArrayList<>();
    public Player opponent;
    public Player player;

    public Game() {
    }

    public HashMap<String,Entity> getEntities() {

        return entityMap;
    }

    public ArrayList<Player> getPlayerList() {
        ArrayList<Player> list =new ArrayList<>();
        for (String id: playerMap.keySet()) {
            list.add(playerMap.get(id));
        }

        return list;
    }

    public Player getPlayer(String playerId) {
        Player player = playerMap.get(playerId);
        if (player == null) {
            player = new Player();
            playerMap.put(playerId, player);
        }

        return player;
    }

    public Entity getEntityUnsafe(String entityID) {
        return entityMap.get(entityID);
    }

    public Entity getEntity(String entityID) {
        Entity entity = entityMap.get(entityID);
        if (entity == null) {
            Timber.e("unknown entity: " + entityID);
            entity = new Entity();
        }

        return entity;
    }

    public void setEntity(Entity entity) {
        entityMap.put(entity.EntityID, entity);
    }

    public void setEntity(String id, Entity entity) {
        entityMap.put(id, entity);
    }
}
