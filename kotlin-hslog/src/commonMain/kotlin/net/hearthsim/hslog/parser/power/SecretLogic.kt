package net.hearthsim.hslog.parser.power

import net.hearthsim.console.Console
import net.hearthsim.hslog.util.AvailableSecrets
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId
import net.hearthsim.hsmodel.enum.Rarity
import net.hearthsim.hsmodel.enum.Type

internal class SecretLogic(val cardJson: CardJson, val console: Console) {
    private val availableSecrets = AvailableSecrets()

    private fun secretEntityList(game: Game): List<Entity> {
        return game.getEntityList { shouldTrack(game, it) }
    }

    private fun shouldTrack(game: Game, e: Entity): Boolean {
        return Entity.ZONE_SECRET == e.tags[Entity.KEY_ZONE]
                && Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] // Legendary secrets are quests, ignore them
                && e.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID

    }

    private fun exclude(game: Game, cardId: String) {
        secretEntityList(game).forEach {
            it.extra.excludedSecretList.add(cardId)
        }
    }

    fun blockPlayed(game: Game, target: String?, playedEntity: Entity) {
        if (shouldTrack(game, playedEntity)) {
            // a secret was just played
            playedEntity.extra.excludedSecretList.clear()
        } else {
            // a card was played
            if (playedEntity.tags[Entity.KEY_CONTROLLER] != game.player?.entity?.PlayerID) {
                // this is a play by the opponent, we don't care
                return
            }

            val numCardsPlayed = game.player?.entity?.tags?.get(Entity.KEY_NUM_CARDS_PLAYED_THIS_TURN)?.toIntOrNull()
            if (numCardsPlayed != null && numCardsPlayed >= 3) {
                exclude(game, CardId.RAT_TRAP)
                exclude(game, CardId.HIDDEN_WISDOM)
            }

            when (playedEntity.tags[Entity.KEY_CARDTYPE]) {
                Type.MINION -> {
                    exclude(game, CardId.SNIPE)
                    exclude(game, CardId.POTION_OF_POLYMORPH)
                    exclude(game, CardId.EXPLOSIVE_RUNES)
                    exclude(game, CardId.REPENTANCE)

                    if (opponentHasRoomInHand(game)) {
                        exclude(game, CardId.FROZEN_CLONE)
                    }
                    if (opponentHasMinionInHand(game)) {
                        exclude(game, CardId.HIDDEN_CACHE)
                    }
                    if (opponentHasRoomOnBoard(game)) {
                        exclude(game, CardId.MIRROR_ENTITY)
                    }
                    if (playerMinionsOnBoard(game) >= 3) {
                        exclude(game, CardId.SACRED_TRIAL)
                    }
                }
                Type.SPELL -> {
                    exclude(game, CardId.COUNTERSPELL)

                    if (playerHasMinionOnBoard(game)) {
                        exclude(game, CardId.PRESSURE_PLATE)
                    }
                    if (opponentHasMinionOnBoard(game)) {
                        exclude(game, CardId.NEVER_SURRENDER)
                    }
                    if (opponentHasRoomOnBoard(game)) {
                        exclude(game, CardId.CAT_TRICK)
                        exclude(game, CardId.NETHERWIND_PORTAL)
                    }
                    if (opponentHasRoomInHand(game)) {
                        exclude(game, CardId.MANA_BIND)
                    }

                    val targetEntity = game.findEntityUnsafe(target!!)
                    if (targetEntity != null && Type.MINION == targetEntity.tags[Entity.KEY_CARDTYPE]) {
                        exclude(game, CardId.SPELLBENDER)
                    }
                }
                Type.HERO_POWER -> {
                    exclude(game, CardId.DART_TRAP)

                }
            }
        }
    }

    private fun playerHasMinionOnBoard(game: Game): Boolean {
        return playerMinionsOnBoard(game) > 0
    }

    private fun playerMinionsOnBoard(game: Game): Int {
        return game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.player?.entity?.PlayerID
                    && it.tags[Entity.KEY_ZONE] == Entity.ZONE_PLAY
                    && it.tags[Entity.KEY_CARDTYPE] == Type.MINION
        }.size
    }

    private fun opponentHasRoomInHand(game: Game): Boolean {
        return game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID
                    && it.tags[Entity.KEY_ZONE] == Entity.ZONE_HAND
        }.size < 10
    }

    private fun opponentHasRoomOnBoard(game: Game): Boolean {
        return opponentMinionOnBoardCount(game) < 7
    }

    private fun opponentHasMinionInHand(game: Game): Boolean {
        return !game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID
                    && it.tags[Entity.KEY_ZONE] == Entity.ZONE_HAND
                    && it.tags[Entity.KEY_CARDTYPE] == Type.MINION
        }.isEmpty()
    }

    private fun opponentHasMinionOnBoard(game: Game): Boolean {
        return opponentMinionOnBoardCount(game) > 0
    }

    private fun opponentMinionOnBoardCount(game: Game): Int {
        return game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID
                    && it.tags[Entity.KEY_ZONE] == Entity.ZONE_PLAY
                    && it.tags[Entity.KEY_CARDTYPE] == Type.MINION
        }.size
    }

    fun blockAttack(game: Game, tag: BlockTag) {
        val entity = tag.Entity
        val target = tag.Target

        if (entity == null) {
            return
        }
        val attackingEntity = game.findEntitySafe(entity)
        if (attackingEntity.tags[Entity.KEY_CONTROLLER] != game.player?.entity?.PlayerID) {
            // not our play
            return
        }

        if (attackingEntity.tags[Entity.KEY_CONTROLLER] == game.player?.entity?.PlayerID) {
            // apparently, freezing trap will trigger even if the player hand is full
            exclude(game, CardId.FREEZING_TRAP)
            if (opponentHasRoomOnBoard(game)) {
                exclude(game, CardId.NOBLE_SACRIFICE)
            }
        }

        if (target == null) {
            return
        }
        val targetEntity = game.findEntitySafe(target)

        if (targetEntity.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID) {
            when (targetEntity.tags[Entity.KEY_CARDTYPE]) {
                Type.MINION -> {
                    if (opponentHasRoomOnBoard(game)) {
                        exclude(game, CardId.VENOMSTRIKE_TRAP)
                        exclude(game, CardId.SNAKE_TRAP)
                        exclude(game, CardId.SPLITTING_IMAGE)
                        exclude(game, CardId.PACK_TACTICS)
                        exclude(game, CardId.BAMBOOZLE)
                    }
                }
                Type.HERO -> {
                    exclude(game, CardId.EXPLOSIVE_TRAP)
                    exclude(game, CardId.MISDIRECTION)
                    exclude(game, CardId.ICE_BARRIER)
                    exclude(game, CardId.EYE_FOR_AN_EYE)

                    if (opponentHasRoomOnBoard(game)) {
                        exclude(game, CardId.BEAR_TRAP)
                        exclude(game, CardId.WANDERING_MONSTER)
                    }

                    if (minionHasNeighbour(game, attackingEntity)) {
                        exclude(game, CardId.SUDDEN_BETRAYAL)
                    }

                    if (attackingEntity.tags[Entity.KEY_CARDTYPE] == Type.MINION) {
                        exclude(game, CardId.VAPORIZE)
                        exclude(game, CardId.FLAME_WARD)
                    }
                }
            }
        }
    }

    private fun minionHasNeighbour(game: Game, attackingEntity: Entity): Boolean {

        try {
            val position = attackingEntity.tags[Entity.KEY_ZONE_POSITION]!!.toInt()

            val totalMinions = game.getEntityList {
                it.tags[Entity.KEY_ZONE] == Entity.ZONE_PLAY
                        && it.tags[Entity.KEY_CARDTYPE] == Type.MINION
                        && it.tags[Entity.KEY_CONTROLLER] == attackingEntity.tags[Entity.KEY_CONTROLLER]
            }.size

            return position > 0 || totalMinions > position + 1
        } catch (e: Exception) {
            return false
        }
    }

    fun damage(game: Game, tag: MetaDataTag) {
        try {
            val damage = tag.Data?.toInt() ?: 0
            if (damage > 0) {
                for (id in tag.Info) {
                    val damagedEntity = game.findEntitySafe(id)

                    if (damagedEntity.tags[Entity.KEY_CONTROLLER] == game.opponent?.entity?.PlayerID
                            && damagedEntity.tags[Entity.KEY_CARDTYPE] == Type.HERO) {
                        exclude(game, CardId.EVASION)
                    }
                }
            }
        } catch (e: Exception) {
            console.error(e)
        }

    }

    fun minionDied(game: Game, entity: Entity) {
        if (entity.tags[Entity.KEY_CONTROLLER] != game.opponent?.entity?.PlayerID) {
            // one of our minions died, we don't care
            return
        }

        if ("0" != game.opponent?.entity?.tags?.get(Entity.KEY_CURRENT_PLAYER)) {
            // secret don't get triggered during the oponent turn
            return
        }

        exclude(game, CardId.EFFIGY)
        exclude(game, CardId.REDEMPTION)

        if (opponentHasRoomInHand(game)) {
            exclude(game, CardId.DUPLICATE)
            exclude(game, CardId.GETAWAY_KODO)
            exclude(game, CardId.GETAWAY_KODO)
        }

        if (opponentHasMinionOnBoard(game)) {
            exclude(game, CardId.AVENGE)
        }
    }

    fun newTurn(game: Game) {
        if (game.opponent?.entity?.tags?.get(Entity.KEY_CURRENT_PLAYER) == "1") {
            if (opponentMinionOnBoardCount(game) > 0) {
                exclude(game, CardId.COMPETITIVE_SPIRIT)
            }
        }
    }

    internal fun getAll(game: Game): List<PossibleSecret> {
        val entities = game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.opponentId()
                && it.tags[Entity.KEY_ZONE] == Entity.ZONE_SECRET
                && it.tags[Entity.KEY_RARITY] != Rarity.LEGENDARY
                && it.tags[Entity.KEY_SIDEQUEST].isNullOrBlank()
        }

        val map = mutableMapOf<String, Int>()
        entities.forEach { entity ->
            availableSecrets.availableSecretsCached(
                cardJson = cardJson,
                playerClass = entity.tags[Entity.KEY_CLASS] ?: "",
                formatType = game.formatType,
                gameType = game.gameType
            ).forEach {
                val possibleCount = map.getOrElse(it, { 0 })

                map.put(it, possibleCount + if (entity.extra.excludedSecretList.contains(it)) 0 else 1)
            }
        }

        return map.map {
            PossibleSecret(it.key, it.value)
        }
    }
}
