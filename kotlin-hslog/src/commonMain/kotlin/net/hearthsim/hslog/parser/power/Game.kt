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

    var victory: Boolean? = null

    var spectator: Boolean = false
    var buildNumber: String? = null
    var gameType: GameType = GameType.GT_RANKED
    var formatType: FormatType = FormatType.FT_UNKNOWN
    var scenarioId: String? = null

    internal var hasSentBattlegroundsHeroes = false
    internal val battlegroundsBoard = mutableMapOf<String, BattlegroundsBoard>()

    /*
     * We patch turn and leaderboard place on the fly
     */
    val battlegroundState: BattlegroundState
        get() {
            val currentTurn = gameEntity?.tags?.get(Entity.KEY_TURN)?.toInt() ?: 0
            val boardsOrdered = battlegroundsBoard.values
                    .map {board ->
                        // There are multiple entities for the same hero
                        // lookup the current leaderboard position
                        val candidates = getEntityList {
                            it.CardID == board.heroCardId
                                    && it.tags.get(Entity.KEY_PLAYER_LEADERBOARD_PLACE) != null
                        }

                        if (candidates.size >1) {
                            console.debug("${candidates.size} candidates to determine leaderboard")
                        }

                        val actual = candidates.firstOrNull()
                        val leaderBoardPlace = actual?.tags?.get(Entity.KEY_PLAYER_LEADERBOARD_PLACE)?.toIntOrNull()

                        //console.debug("CardId=${actual?.CardID} entity=${actual?.EntityID} leaderBoardPlace=$leaderBoardPlace")

                        board.copy(
                                currentTurn = currentTurn,
                                leaderboardPlace = leaderBoardPlace ?: 8
                                )
                    }.sortedBy {
                        it.leaderboardPlace
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
