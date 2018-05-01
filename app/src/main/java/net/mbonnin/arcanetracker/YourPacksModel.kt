package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.room.RPack

sealed class Item

class PacksItem(val packs: Int): Item()
class DustItem(val dust: Int): Item()
class DustAverageItem(val average: Int): Item()
class PityCounterItem(val rarity: String): Item()
class PackItem(val rpack: RPack): Item()