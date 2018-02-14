package net.mbonnin.arcanetracker.adapter

import net.mbonnin.arcanetracker.parser.EntityList
import net.mbonnin.hsmodel.Card


class DeckEntryItem(var card: Card, var count: Int = 0, var gift: Boolean = false , var entityList: EntityList = EntityList())