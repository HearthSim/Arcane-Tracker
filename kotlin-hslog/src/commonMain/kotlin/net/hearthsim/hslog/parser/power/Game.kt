package net.hearthsim.hslog.parser.power

import net.hearthsim.console.Console


class Game(private val console: Console) {

    var entityMap = HashMap<String, Entity>()
    var opponentBattleTag: String? = null

    internal var playerMap = HashMap<String, Player>()
    var gameEntity: Entity? = null

    var player: Player? = null
    /**
     * For battlegrounds, the opponent will be BACON_DUMMY_PLAYER
     */
    var opponent: Player? = null

    var playerRank: Int = 0
    var opponentRank: Int = 0

    var victory: Boolean = false

    var spectator: Boolean = false
    var buildNumber: String? = null
    var gameType: GameType = GameType.GT_RANKED
    var formatType: FormatType = FormatType.FT_UNKNOWN
    var scenarioId: String? = null

    internal val battlegroundsBoard = mutableMapOf<String, BattlegroundBoard>()

    val battlegroundState: BattlegroundState
        get() {
            val boardsOrdered = battlegroundsBoard.values.sortedBy { board ->
                board.opponentHero.tags.get(Entity.KEY_PLAYER_LEADERBOARD_PLACE)?.toInt() ?: 0
            }
            return BattlegroundState(boardsOrdered)
        }

    val isStarted: Boolean
        get() = player != null && opponent != null

    fun findController(entity: Entity): Player {
        val playerId = entity.tags[Entity.KEY_CONTROLLER]
        if (playerId == null) {
            console.error("cannot find playerId for $entity")
            /**
             * do not crash...
             */
            return Player()
        }

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

    fun playerId() = player!!.entity!!.PlayerID
    fun opponentId() = opponent!!.entity!!.PlayerID
}
