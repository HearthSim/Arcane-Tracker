package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hsmodel.Card


sealed class DeckEntry {
    class Item(var card: Card, var count: Int = 0, var gift: Boolean = false , var entityList: List<Entity>): DeckEntry()
    class PlayerDeck: DeckEntry()
    class Unknown(val count: Int) : DeckEntry()
    class Hand(val count: Int) : DeckEntry()
    class OpponentDeck : DeckEntry()
    class Secrets: DeckEntry()
}