package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.BattlegroundsBoard
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hsmodel.Card


sealed class DeckEntry {
    object PlayerDeck: DeckEntry()
    object OpponentDeck : DeckEntry()
    object Secrets: DeckEntry()

    class Text(val text: String): DeckEntry()
    class Item(var card: Card,
               var count: Int = 0,
               var gift: Boolean = false ,
               var entityList: List<Entity>,
               val techLevel: Int? = null
    ): DeckEntry()
    class Hero(val card: Card, val board: BattlegroundsBoard): DeckEntry()
    class Unknown(val count: Int) : DeckEntry()
    class Hand(val count: Int) : DeckEntry()
}