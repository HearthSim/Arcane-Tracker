package net.mbonnin.arcanetracker

import net.hearthsim.hslog.Deck
import net.hearthsim.hslog.parser.decks.DeckStringHelper
import net.mbonnin.arcanetracker.room.RDeck

object DeckMapper {
    fun fromRDeck(rdeck: RDeck): Deck? {
        val deck = DeckStringHelper.parse(rdeck.deck_string, ArcaneTrackerApplication.get().cardJson)
        if (deck == null) {
            return null
        }

        deck.wins = rdeck.wins
        deck.losses = rdeck.losses
        deck.name = rdeck.name
        deck.id = rdeck.id
        return deck
    }
}