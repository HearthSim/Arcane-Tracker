package net.mbonnin.arcanetracker.parser;

import com.annimon.stream.function.Predicate;

import net.mbonnin.arcanetracker.Utils;

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
    public Entity gameEntity;

    public Player player;
    public Player opponent;

    public ArrayList<Play> plays = new ArrayList<>();
    public boolean victory;
    public int bnetGameType;
    public String lastPlayedCardId;
    public int rank = -1;

    public Player getPlayer() {
        return player;
    }
    public Player getOpponent() {
        return opponent;
    }

    public Player findController(Entity entity) {
        return findPlayer(entity.tags.get(Entity.KEY_CONTROLLER));
    }

    public Entity findControllerEntity(Entity entity) {
        String playerId = entity.tags.get(Entity.KEY_CONTROLLER);
        for (Entity e: entityMap.values()) {
            if (Utils.INSTANCE.equalsNullSafe(e.PlayerID, playerId)) {
                return e;
            }
        }
        return null;
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

    public EntityList getEntityList(Predicate<Entity> predicate) {
        EntityList entityList = new EntityList();
        for (Entity entity: entityMap.values()) {
            if (predicate.test(entity)) {
                entityList.add(entity);
            }
        }
        return entityList;
    }

    public boolean isStarted() {
        return player != null && opponent != null;
    }

    public void addEntity(Entity entity) {
        entityMap.put(entity.EntityID, entity);
    }

    public Entity findEntitySafe(String IdOrBattleTag) {
        Entity entity;

        entity = entityMap.get(IdOrBattleTag);
        if (entity != null) {
            return entity;
        }

        if ("GameEntity".equals(IdOrBattleTag)) {
            return gameEntity;
        }

        if (Utils.INSTANCE.isEmpty(IdOrBattleTag)) {
            return unknownEntity("empty");
        }

        // this must be a battleTag
        entity = entityMap.get(IdOrBattleTag);
        if (entity == null) {
            Timber.w("Adding battleTag " + IdOrBattleTag);
            if (battleTags.size() >= 2) {
                Timber.e("[Inconsistent] too many battleTags");
            }
            battleTags.add(IdOrBattleTag);

            entity = new Entity();
            entity.EntityID = IdOrBattleTag;
            entityMap.put(IdOrBattleTag, entity);
        }
        return entity;
    }

    private Entity unknownEntity(String entityId) {
        Timber.e("unknown entity " + entityId);
        Entity entity = new Entity();
        entity.EntityID = entityId;
        return entity;
    }

    public Entity findEntityUnsafe(String entityId) {
        return entityMap.get(entityId);
    }
}
