package net.hearthsim.hslog.parser.power

import net.hearthsim.console.Console


class Game(private val console: Console) {

    var entityMap = HashMap<String, Entity>()
    var opponentBattleTag: String? = null

    internal var playerMap = HashMap<String, Player>()
    var gameEntity: Entity? = null

    var player: Player? = null
    var opponent: Player? = null

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

    fun findPlayer(playerId: String): Player {
        val player = playerMap[playerId]
        if (player == null) {
            console.error("cannot find player $playerId")
            /**
             * do not crash...
             */
            return Player()
        }
        return player
    }

    fun getEntityList(predicate: (Entity) -> Boolean): List<Entity> {
        return entityMap.values.filter(predicate)
    }

    fun addEntity(entity: Entity) {
        entityMap[entity.EntityID!!] = entity
    }

    fun findEntitySafe(IdOrBattleTag: String): Entity {
        var entity = entityMap[IdOrBattleTag]
        if (entity != null) {
            return entity
        }

        if ("GameEntity" == IdOrBattleTag) {
            return gameEntity ?: unknownEntity("game")
        }

        if (IdOrBattleTag.isEmpty()) {
            return unknownEntity("empty")
        }

        // this must be a battleTag
        entity = entityMap[IdOrBattleTag]
        if (entity == null) {
            if (opponentBattleTag != null) {
                console.error("Already added opponent battleTag")
                entity = unknownEntity("empty")
            } else {
                entity = entityMap.get("UNKNOWN HUMAN PLAYER")

                if (entity == null) {
                    console.error("UNKNOWN HUMAN PLAYER not set ?")
                    entity = unknownEntity("empty")
                }

                console.debug("Found opponent battleTag=$IdOrBattleTag, make it point to ${entity.EntityID}")

                entityMap[IdOrBattleTag] = entity

                opponentBattleTag = IdOrBattleTag
            }
        }
        return entity
    }

    private fun unknownEntity(entityId: String): Entity {
        console.error("unknown entity $entityId")
        val entity = Entity()
        entity.EntityID = entityId
        return entity
    }

    fun findEntityUnsafe(entityId: String): Entity? {
        return entityMap[entityId]
    }
}
