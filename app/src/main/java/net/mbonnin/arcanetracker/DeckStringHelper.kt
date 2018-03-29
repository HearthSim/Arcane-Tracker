package net.mbonnin.arcanetracker

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
        fun parse(pasteData: String): Deck? {
            val deckStringParser = DeckStringHelper()
            val lines = pasteData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (line in lines) {
                val result = deckStringParser.parseLine(line)
                if (result != null) {

                    val deck = DeckStringParser.parse(result.deckString)

                    if (deck != null) {
                        deck.name = result.name ?: "Imported Deck"
                        deck.id = result.id ?: UUID.randomUUID().toString().replace("-", "")
                        return deck
                    }
                }
            }

            return null
        }
    }
}
