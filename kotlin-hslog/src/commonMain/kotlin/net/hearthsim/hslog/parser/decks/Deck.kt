package net.hearthsim.hslog.parser.decks

import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson

class Deck private constructor(
        /**
         * a map of cardId to number of cards for this cardId
         */
        val cards: Map<String, Int>,
        /**
         * the classIndex
         */
        val classIndex: Int,
        var name: String?,
        var id: String?,
        var wins: Int,
        var losses: Int
) {
    companion object {
        const val MAX_CARDS = 30

        fun create(cards: Map<String, Int>,
                   classIndex: Int,
                   name: String? = null,
                   id: String? = null,
                   wins: Int = 0,
                   losses: Int = 0,
                   cardJson: CardJson): Deck {

            var actualClassIndex = classIndex
            for (cardId in cards.keys) {
                val card = cardJson.getCard(cardId)
                val ci = getClassIndex(card.playerClass)

                if (ci != actualClassIndex && ci >= 0 && ci < Card.CLASS_INDEX_NEUTRAL) {
                    actualClassIndex = ci
                    break
                }
            }
            //Timber.e("inconsistent class index, force to" + getPlayerClass(ci))

            return Deck(cards = cards,
                    classIndex = actualClassIndex,
                    name = name,
                    id = id,
                    wins = wins,
                    losses = losses
                    )
        }
    }
}
