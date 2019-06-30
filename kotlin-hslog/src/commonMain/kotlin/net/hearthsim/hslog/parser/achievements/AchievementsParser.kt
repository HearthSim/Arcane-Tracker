package net.hearthsim.hslog.parser.achievements

import net.hearthsim.console.Console

class AchievementsParser(val console: Console, val onCard: (CardGained) -> Unit) {
    private val CARD_GAINED = Regex(".*NotifyOfCardGained:.*cardId=(.*) .* (.*) [0-9]*")

    class CardGained(val id: String, val golden: Boolean) {
        override fun toString(): String {
            if (golden) {
                return "$id*"
            } else {
                return id
            }
        }
    }

    fun process(rawLine: String, isOldData: Boolean) {
        if (isOldData) {
            return
        }
        //D 20:44:22.9979440 NotifyOfCardGained: [name=Eviscerate cardId=EX1_124 type=SPELL] NORMAL 3
        console.debug(rawLine)
        val matcher = CARD_GAINED.matchEntire(rawLine)

        if (matcher != null) {
            val cardId = matcher.groupValues[1]
            val isGolden = matcher.groupValues[2] == "GOLDEN"

            console.debug("Opened card: ${matcher.groupValues[1]}")
            onCard(CardGained(cardId, isGolden))
        }
    }

}