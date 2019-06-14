package net.mbonnin.arcanetracker.hslog.decks

import decodeBase64
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.IoBuffer
import net.hearthsim.kotlin.deckstring.Deckstring
import net.mbonnin.arcanetracker.hslog.Deck
import net.mbonnin.arcanetracker.hslog.util.getClassIndex
import net.mbonnin.hsmodel.CardJson
import java.nio.ByteBuffer

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
                e.printStackTrace()
                return null
            }
        }

        private fun parseUnsafe(deckstring: String, cardJson: CardJson): Deck? {
            val deck = Deck()

            val data = deckstring.toByteArray().decodeBase64()

            val result = Deckstring.decode(ByteReadPacket(data))

            deck.classIndex = result.heroes.map {
                val card = cardJson.getCard(it)
                card?.playerClass?.let { getClassIndex(it) } ?: -1
            }.firstOrNull() ?: -1

            if (deck.classIndex < 0) {
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

            deck.cards = HashMap(map)

            return deck
        }
    }
}
