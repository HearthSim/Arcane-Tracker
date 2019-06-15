package net.hearthsim.hslog.parser.decks

import net.hearthsim.deckstring.Deckstring
import net.hearthsim.hslog.Deck
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hsmodel.CardJson

class DeckStringHelper {
    var name: String? = null
    var id: String? = null

    class Result(val name: String?, val id: String?, val deckString: String)

    fun parseLine(line: String): Result? {
        if (line.startsWith("### ")) {
            name = line.substring(4)
        } else if (line.startsWith("# Deck ID: ")) {
            id = line.substring("# Deck ID: ".length)
        } else if (!line.startsWith("#")) {
            return Result(name, id, line)
        }

        return null
    }

    companion object {
        fun parse(deckstring: String, cardJson: CardJson): Deck? {
            try {
                return parseUnsafe(deckstring, cardJson)
            } catch (e: Exception) {
                return null
            }
        }

        private fun parseUnsafe(deckstring: String, cardJson: CardJson): Deck? {
            val result = Deckstring.decode(deckstring)

            val classIndex = result.heroes.map {
                val card = cardJson.getCard(it)
                card?.playerClass?.let { getClassIndex(it) } ?: -1
            }.firstOrNull() ?: -1

            if (classIndex < 0) {
                return null
            }

            val map = result.cards.map {
                val card = cardJson.getCard(it.dbfId)

                if (card == null) {
                    null
                } else {
                    card.id to it.count
                }
            }.filterNotNull()
                    .toMap()

            return Deck.create(cards = map,
                    classIndex = classIndex,
                    cardJson = cardJson)
        }
    }
}
