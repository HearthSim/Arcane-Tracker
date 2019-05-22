package net.mbonnin.arcanetracker.helper

import android.util.Base64
import net.hearthsim.java.deckstrings.Deckstrings
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.hslog.Deck
import java.util.*

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
        fun parse(deckstring: String): Deck? {
            try {
                return parseUnsafe(deckstring)
            } catch (e: Exception) {
                return null
            }
        }

        private fun parseUnsafe(deckstring: String): Deck? {
            val deck = Deck()

            val data = Base64.decode(deckstring, Base64.DEFAULT)
            val result = Deckstrings.decode(data)

            deck.classIndex = result.heroes.map {
                val card = CardUtil.getCard(it);
                card?.playerClass?.let { getClassIndex(it) } ?: -1
            }.firstOrNull() ?: -1

            if (deck.classIndex < 0) {
                return null
            }

            val map = result.cards.map {
                val card = CardUtil.getCard(it.dbfId)

                if (card == null) {
                    null
                } else {
                    card.id to it.count
                }
            }.filterNotNull()
                    .toMap()

            deck.cards = HashMap(map)

            return deck
        }
    }
}
