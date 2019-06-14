package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.hslog.Deck
import net.mbonnin.arcanetracker.hslog.decks.DeckStringHelper
import net.mbonnin.arcanetracker.room.RDeck
import net.hearthsim.hsmodel.CardJson

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