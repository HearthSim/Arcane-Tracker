package net.mbonnin.arcanetracker.ui.overlay.adapter

import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hsmodel.Card


class DeckEntryItem(var card: Card, var count: Int = 0, var gift: Boolean = false , var entityList: List<Entity>)