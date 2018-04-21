package net.mbonnin.arcanetracker.parser

import net.mbonnin.arcanetracker.Utils
import timber.log.Timber
import java.util.*

class Game {

    var entityMap = HashMap<String, Entity>()
    var battleTags = ArrayList<String>()

    internal var playerMap = HashMap<String, Player>()
    var gameEntity: Entity? = null

    var player: Player? = null
    var opponent: Player? = null

    var plays = ArrayList<Play>()
    var victory: Boolean = false

    var lastPlayedCardId: String? = null
    var spectator: Boolean = false
    var buildNumber: String? = null
    var gameType: String? = null
    var formatType: String? = null
    var scenarioId: String? = null
    var opponentRank: Int = 0
    var playerRank: Int = 0

    val isStarted: Boolean
        get() = player != null && opponent != null

    fun findController(entity: Entity): Player {
        return findPlayer(entity.tags[Entity.KEY_CONTROLLER]!!)
    }

    fun findControllerEntity(entity: Entity): Entity? {
        val playerId = entity.tags[Entity.KEY_CONTROLLER]
        for (e in entityMap.values) {
            if (Utils.equalsNullSafe(e.PlayerID, playerId)) {
                return e
            }
        }
        return null
    }

    fun findPlayer(playerId: String): Player {
        val player = playerMap[playerId]
        if (player == null) {
            Timber.e("cannot find player $playerId")
            /**
             * do not crash...
             */
            return Player()
        }
        return player
    }

    fun getEntityList(predicate: (Entity) -> Boolean): EntityList {
        val entityList = EntityList()
        for (entity in entityMap.values) {
            if (predicate.invoke(entity)) {
                entityList.add(entity)
            }
        }
        return entityList
    }

    fun addEntity(entity: Entity) {
        entityMap[entity.EntityID!!] = entity
    }

    fun findEntitySafe(IdOrBattleTag: String): Entity? {
        var entity: Entity?

        entity = entityMap[IdOrBattleTag]
        if (entity != null) {
            return entity
        }

        if ("GameEntity" == IdOrBattleTag) {
            return gameEntity
        }

        if (Utils.isEmpty(IdOrBattleTag)) {
            return unknownEntity("empty")
        }

        // this must be a battleTag
        entity = entityMap[IdOrBattleTag]
        if (entity == null) {
            Timber.w("Adding battleTag $IdOrBattleTag")
            if (battleTags.size >= 2) {
                Timber.e("[Inconsistent] too many battleTags")
            }
            battleTags.add(IdOrBattleTag)

            entity = Entity()
            entity.EntityID = IdOrBattleTag
            entityMap[IdOrBattleTag] = entity
        }
        return entity
    }

    private fun unknownEntity(entityId: String): Entity {
        Timber.e("unknown entity $entityId")
        val entity = Entity()
        entity.EntityID = entityId
        return entity
    }

    fun findEntityUnsafe(entityId: String): Entity? {
        return entityMap[entityId]
    }
}
