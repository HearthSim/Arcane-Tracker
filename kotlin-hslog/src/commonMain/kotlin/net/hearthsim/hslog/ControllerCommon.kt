package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hsmodel.CardJson

object ControllerCommon {
    class Intermediate(val cardId: String?, val entity: Entity)

    private data class GroupingKey(val cardId: String?, val gift: Boolean)

    val deckEntryComparator = Comparator<DeckEntry.Item> { a, b ->
        val acost = a.card.cost
        val bcost = b.card.cost

        if (acost == null && bcost != null) {
            return@Comparator -1
        } else if (acost != null && bcost == null) {
            return@Comparator 1
        } else if (acost != null && bcost != null) {
            val r = acost - bcost
            if (r != 0) {
                return@Comparator r
            }
        }

        val r = a.card.name.compareTo(b.card.name)
        if (r != 0) {
            return@Comparator r
        }

        val agift = if (a.gift) 1 else 0
        val bgift = if (b.gift) 1 else 0
        return@Comparator agift - bgift
    }

    fun intermediateToDeckEntryList(intermediateList: List<Intermediate>, increasesCount: (Entity) -> Boolean, cardJson: CardJson): List<DeckEntry> {
        val map = intermediateList.groupBy({ GroupingKey(it.cardId, !it.entity.extra.createdBy.isNullOrEmpty()) }, { it.entity })

        val deckEntryList = map.map {
            val cardId = it.key.cardId
            val card = if (cardId == null) {
                CardJson.UNKNOWN
            } else {
                cardJson.getCard(cardId)
            }
            val entityList = it.value

            val count = entityList
                    .map { if (increasesCount(it)) 1 else 0 }
                    .sum()

            DeckEntry.Item(card,
                    gift = it.key.gift,
                    count = count,
                    entityList = entityList)
        }

        return deckEntryList.sortedWith(deckEntryComparator)
    }
}