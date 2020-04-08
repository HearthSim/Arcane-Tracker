package net.hearthsim.hslog.util

import net.hearthsim.hsmodel.enum.PlayerClass

fun allPlayerClasses(): Array<String> {
    return arrayOf(PlayerClass.WARRIOR,
        PlayerClass.SHAMAN,
        PlayerClass.ROGUE,
        PlayerClass.PALADIN,
        PlayerClass.HUNTER,
        PlayerClass.DRUID,
        PlayerClass.WARLOCK,
        PlayerClass.MAGE,
        PlayerClass.PRIEST,
        PlayerClass.DEMONHUNTER,
        PlayerClass.NEUTRAL)
}

fun getPlayerClass(classIndex: Int): String {
    if (classIndex < 0) {
        return PlayerClass.NEUTRAL
    }
    return allPlayerClasses()[classIndex]
}

fun getClassIndex(playerClass: String): Int {
    return allPlayerClasses().indexOf(playerClass)
}