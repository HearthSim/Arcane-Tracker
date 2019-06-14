package net.hearthsim.hslog.util

import net.hearthsim.hsmodel.enum.PlayerClass

fun allHeroes(): Array<String> {
    return arrayOf(PlayerClass.WARRIOR, PlayerClass.SHAMAN, PlayerClass.ROGUE, PlayerClass.PALADIN, PlayerClass.HUNTER, PlayerClass.DRUID, PlayerClass.WARLOCK, PlayerClass.MAGE, PlayerClass.PRIEST, PlayerClass.NEUTRAL)
}

fun getPlayerClass(classIndex: Int): String {
    if (classIndex < 0) {
        return PlayerClass.NEUTRAL
    }
    return allHeroes()[classIndex]
}

fun getClassIndex(playerClass: String): Int {
    return allHeroes().indexOf(playerClass)
}