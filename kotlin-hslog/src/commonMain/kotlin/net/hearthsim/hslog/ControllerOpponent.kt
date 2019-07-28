package net.hearthsim.hslog

import net.hearthsim.console.Console
import net.hearthsim.hslog.ControllerCommon.intermediateToDeckEntryList
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.GameLogic
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.HSSet
import net.hearthsim.hsmodel.enum.PlayerClass
import net.hearthsim.hsmodel.enum.Rarity
import net.hearthsim.hsmodel.enum.Type

class ControllerOpponent(private val console: Console, private val cardJson: CardJson) {
    private fun opponentHand(game: Game, opponentId: String): List<DeckEntry> {
        val list = mutableListOf<DeckEntry>()

        val entities = getEntityListInZone(game, opponentId, Entity.ZONE_HAND)
                .sortedBy { it.tags[Entity.KEY_ZONE_POSITION] }

        for (entity in entities) {
            val card = entity.card
            val deckEntry = if (card == null || entity.extra.hide) {
                val builder = StringBuilder()
                builder.append("#").append(GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn))
                if (entity.extra.mulliganed) {
                    builder.append(" (M)")
                }
                val drawTurn = GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn)
                val mulliganed = if (entity.extra.mulliganed) " (M)" else ""

                val displayedEntity = Entity()
                displayedEntity.extra.drawTurn = entity.extra.drawTurn

                DeckEntry.Item(
                        card = CardJson.unknown("#${drawTurn}${mulliganed}"),
                        gift = false,
                        count = 1,
                        entityList = listOf(displayedEntity)
                )
            } else {
                DeckEntry.Item(
                        card = card,
                        gift = !entity.extra.createdBy.isNullOrEmpty(),
                        count = 1,
                        entityList = listOf(entity)
                )
            }

            list.add(deckEntry)
        }

        return list
    }

    fun secret(playerClass: String): Card {
        val id: String
        val cost: Int
        val pClass: String

        when (playerClass) {
            PlayerClass.PALADIN -> {
                id = "secret_p"
                cost = 1
                pClass = PlayerClass.PALADIN
            }
            PlayerClass.HUNTER -> {
                id = "secret_h"
                cost = 2
                pClass = PlayerClass.HUNTER
            }
            PlayerClass.ROGUE -> {
                id = "secret_r"
                cost = 2
                pClass = PlayerClass.ROGUE
            }
            PlayerClass.MAGE -> {
                id = "secret_m"
                cost = 3
                pClass = PlayerClass.MAGE
            }
            else -> return CardJson.unknown()
        }

        return Card(
                type = Type.SPELL,
                text = "Secret",
                name = "Secret",
                id = id,
                cost = cost,
                playerClass = pClass,
                dbfId = 0,
                set = HSSet.CORE
        )
    }


    fun getSecrets(game: Game, opponentId: String): List<DeckEntry.Item> {
        val list = ArrayList<DeckEntry.Item>()

        val entities = getEntityListInZone(game, opponentId, Entity.ZONE_SECRET)
                .filter { e -> Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] }
                .sortedBy { it.tags[Entity.KEY_ZONE_POSITION] }

        for (entity in entities) {
            val card = if (entity.CardID.isNullOrBlank()) {
                val clazz = entity.tags[Entity.KEY_CLASS]

                if (clazz != null) {
                    secret(clazz)
                } else {
                    secret("MAGE")
                }
            } else {
                entity.card
            }

            val clone = entity.clone()
            clone.card = card
            val deckEntry = DeckEntry.Item(
                    card = card!!,
                    gift = !entity.extra.createdBy.isNullOrEmpty(),
                    count = 1,
                    entityList = listOf(clone)
            )
            list.add(deckEntry)
        }

        return list
    }

    private fun getEntityListInZone(game: Game, playerId: String?, zone: String): List<Entity> {
        return game.getEntityList { entity -> playerId == entity.tags[Entity.KEY_CONTROLLER] && zone == entity.tags[Entity.KEY_ZONE] }
    }


    fun getDeckEntries(game: Game): List<DeckEntry> {
        val list = ArrayList<DeckEntry>()
        val opponentId = game.opponent?.entity?.PlayerID!!

        val secrets = getSecrets(game, opponentId)
        if (secrets.isNotEmpty()) {
            list.add(DeckEntry.Secrets)
            list.addAll(secrets)
        }

        val handDeckEntryItemList = opponentHand(game, opponentId)
        list.add(DeckEntry.Hand(handDeckEntryItemList.size))
        list.addAll(handDeckEntryItemList)

        list.add(DeckEntry.OpponentDeck)
        // trying a definition that's a bit different from the player definition here
        val allEntities = game.getEntityList { e ->
            (opponentId == e.tags[Entity.KEY_CONTROLLER]
                    && Type.ENCHANTMENT != e.tags[Entity.KEY_CARDTYPE]
                    && Type.HERO != e.tags[Entity.KEY_CARDTYPE]
                    && Type.HERO_POWER != e.tags[Entity.KEY_CARDTYPE]
                    && "PLAYER" != e.tags[Entity.KEY_CARDTYPE])
        }

        // the logic is a bit different than in opponentHand(). Here we want to display when the card
        // was draw (think prince maltezaar)
        val sanitizedEntities = allEntities.map {
            if (it.extra.hide) {
                val displayedEntity = Entity()
                displayedEntity.CardID = it.CardID
                displayedEntity.card = it.card
                displayedEntity
            } else {
                it
            }
        }

        val intermediateList = mutableListOf<ControllerCommon.Intermediate>()
        var unknownCards = 0
        sanitizedEntities.forEach {
            if (it.CardID == null && it.extra.createdBy.isNullOrEmpty()) {
                unknownCards++
            } else {
                intermediateList.add(ControllerCommon.Intermediate(it.CardID, it))
            }
        }

        list.addAll(intermediateToDeckEntryList(intermediateList, { true }, cardJson))
        if (unknownCards > 0) {
            list.add(DeckEntry.Unknown(unknownCards))
        }
        return list
    }
}