package net.mbonnin.arcanetracker.ui.overlay.adapter

import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.hsmodel.Card


class DeckEntryItem(var card: Card, var count: Int = 0, var gift: Boolean = false , var entityList: List<Entity>)