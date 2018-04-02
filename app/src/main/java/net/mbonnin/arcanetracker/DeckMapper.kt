package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.room.RDeck

object DeckMapper {
    fun fromRDeck(rdeck: RDeck): Deck? {
        val deck = DeckStringParser.parse(rdeck.deck_string)
        if (deck == null) {
            return deck
        }

        deck.name = rdeck.name
        deck.id = rdeck.id
        return deck
    }
}