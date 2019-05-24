package net.mbonnin.arcanetracker.helper

import android.graphics.drawable.Drawable
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.hsmodel.enum.PlayerClass.DRUID
import net.mbonnin.hsmodel.enum.PlayerClass.HUNTER
import net.mbonnin.hsmodel.enum.PlayerClass.MAGE
import net.mbonnin.hsmodel.enum.PlayerClass.NEUTRAL
import net.mbonnin.hsmodel.enum.PlayerClass.PALADIN
import net.mbonnin.hsmodel.enum.PlayerClass.PRIEST
import net.mbonnin.hsmodel.enum.PlayerClass.ROGUE
import net.mbonnin.hsmodel.enum.PlayerClass.SHAMAN
import net.mbonnin.hsmodel.enum.PlayerClass.WARLOCK
import net.mbonnin.hsmodel.enum.PlayerClass.WARRIOR

fun allHeroes(): Array<String> {
    return arrayOf(WARRIOR, SHAMAN, ROGUE, PALADIN, HUNTER, DRUID, WARLOCK, MAGE, PRIEST, NEUTRAL)
}

fun getDisplayName(classIndex: Int): String {
    val index = sanitizeIndex(classIndex)

    when(allHeroes()[index]) {
        WARRIOR -> return Utils.getString(R.string.warrior)
        SHAMAN -> return Utils.getString(R.string.shaman)
        ROGUE -> return Utils.getString(R.string.rogue)
        PALADIN -> return Utils.getString(R.string.paladin)
        HUNTER -> return Utils.getString(R.string.hunter)
        DRUID -> return Utils.getString(R.string.druid)
        WARLOCK -> return Utils.getString(R.string.warlock)
        MAGE -> return Utils.getString(R.string.mage)
        PRIEST -> return Utils.getString(R.string.priest)
        else -> return Utils.getString(R.string.neutral)
    }
}

fun sanitizeIndex(classIndex: Int): Int {
    if (classIndex < 0) {
        return allHeroes().size - 1
    } else if (classIndex >= allHeroes().size) {
        return allHeroes().size - 1
    }
    return  classIndex
}

fun getPlayerClass(classIndex: Int): String {
    return allHeroes()[sanitizeIndex(classIndex)]
}

fun getClassIndex(playerClass: String): Int {
    return allHeroes().indexOf(playerClass)
}

fun getHeroId(classIndex: Int): String {
    return String.format("hero_%02d", sanitizeIndex(classIndex) + 1)
}

object HeroUtil {
    fun getDrawable(playerClass: String): Drawable {
        return Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", getClassIndex(playerClass) + 1))
    }
}