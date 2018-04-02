package net.mbonnin.arcanetracker

import android.graphics.drawable.Drawable
import net.mbonnin.hsmodel.PlayerClass.DRUID
import net.mbonnin.hsmodel.PlayerClass.HUNTER
import net.mbonnin.hsmodel.PlayerClass.MAGE
import net.mbonnin.hsmodel.PlayerClass.NEUTRAL
import net.mbonnin.hsmodel.PlayerClass.PALADIN
import net.mbonnin.hsmodel.PlayerClass.PRIEST
import net.mbonnin.hsmodel.PlayerClass.ROGUE
import net.mbonnin.hsmodel.PlayerClass.SHAMAN
import net.mbonnin.hsmodel.PlayerClass.WARLOCK
import net.mbonnin.hsmodel.PlayerClass.WARRIOR
import java.util.regex.Pattern

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

fun heroIdToClassIndex(heroId: String): Int {

    val pattern = Pattern.compile("hero_([0-9]*)[a-zA-Z]*", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(heroId)
    if (matcher.matches()) {
        try {
            var i = Integer.parseInt(matcher.group(1))
            i--

            if (i >= 0 && i < 9) {
                return i
            }

        } catch (e: Exception) {
        }

    }

    return -1
}

object HeroUtil {
    fun getDrawable(playerClass: String): Drawable {
        return Utils.getDrawableForName(String.format("hero_%02d_round", getClassIndex(playerClass) + 1))
    }
}