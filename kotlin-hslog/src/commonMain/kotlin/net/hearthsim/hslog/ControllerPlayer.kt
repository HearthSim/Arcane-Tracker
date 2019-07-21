package net.hearthsim.hslog

import net.hearthsim.console.Console
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson

class ControllerPlayer(private val console: Console, private val cardJson: CardJson) {
    var playerCardMap: Map<String, Int>? = null

    fun getDeckEntries(game: Game): List<DeckEntry> {
        return getDeckEntries(game, playerCardMap ?: emptyMap())
    }

    private fun getDeckEntries(game: Game, cardMap: Map<String, Int>): List<DeckEntry> {
        val list = mutableListOf<DeckEntry>()
        val playerId = game.player?.entity?.PlayerID!!

        list.add(DeckEntry.PlayerDeck())

        val originalDeckEntityList = game.getEntityList { entity -> playerId == entity.extra.originalController }

        val knownIdList = ArrayList<String>()

        /*
         * build a list of all the ids that we know from the deck or from whizbang
         */
        for ((key, value) in cardMap) {
            for (i in 0 until value) {
                knownIdList.add(key)
            }
        }

        /*
         * remove the ones that have been revealed already
         */
        val revealedEntityList = originalDeckEntityList.filter { !it.CardID.isNullOrBlank() }
        for (entity in revealedEntityList) {
            val it = knownIdList.iterator()
            while (it.hasNext()) {
                val next = it.next()
                if (next == entity.CardID) {
                    it.remove()
                    break
                }
            }
        }

        /*
         * add the revealed cards
         */
        val intermediateList = mutableListOf<ControllerCommon.Intermediate>()
        intermediateList.addAll(revealedEntityList.map { ControllerCommon.Intermediate(it.CardID, it) })

        /*
         * add the known cards from the deck, assume they are still inside the deck
         */
        intermediateList.addAll(knownIdList.map { ControllerCommon.Intermediate(it, Entity.UNKNOWN_ENTITY) })

        /*
         * Add all the gifts
         * XXX it's not enough to filter on !TextUtils.isEmpty(createdBy)
         * because then we get all enchantments
         * if a gift is in the graveyard, it won't be shown but I guess that's ok
         */
        val giftList = game.getEntityList { entity ->
            Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]
                    && playerId != entity.extra.originalController
                    && playerId == entity.tags[Entity.KEY_CONTROLLER]
        }

        intermediateList.addAll(giftList.map { ControllerCommon.Intermediate(it.CardID, it) })

        val increaseCount: (Entity) -> Boolean = { it == Entity.UNKNOWN_ENTITY || it.tags[Entity.KEY_ZONE] == Entity.ZONE_DECK }
        list.addAll(ControllerCommon.intermediateToDeckEntryList(intermediateList, increaseCount, cardJson))

        /*
         * and the unknown if any
         */
        val unknownCards = originalDeckEntityList.size - revealedEntityList.size - knownIdList.size
        if (unknownCards > 0) {
            list.add(DeckEntry.Unknown(unknownCards))
        }
        if (unknownCards < 0) {
            console.error("too many known card ids: $unknownCards")
        }

        return list
    }
}