package net.mbonnin.arcanetracker.helper

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import net.hearthsim.hslog.util.allPlayerClasses
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hsmodel.enum.PlayerClass
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils

fun getPlayerClassDisplayName(playerClass: String): String {
    return when (playerClass) {
        PlayerClass.WARRIOR -> Utils.getString(R.string.warrior)
        PlayerClass.SHAMAN -> Utils.getString(R.string.shaman)
        PlayerClass.ROGUE -> Utils.getString(R.string.rogue)
        PlayerClass.PALADIN -> Utils.getString(R.string.paladin)
        PlayerClass.HUNTER -> Utils.getString(R.string.hunter)
        PlayerClass.DRUID -> Utils.getString(R.string.druid)
        PlayerClass.WARLOCK -> Utils.getString(R.string.warlock)
        PlayerClass.MAGE -> Utils.getString(R.string.mage)
        PlayerClass.PRIEST -> Utils.getString(R.string.priest)
        else -> Utils.getString(R.string.neutral)
    }
}

@DrawableRes fun getPlayerClassRoundIcon(playerClass: String): Int {
    return when (playerClass) {
        PlayerClass.WARRIOR -> R.drawable.hero_01_round
        PlayerClass.SHAMAN -> R.drawable.hero_02_round
        PlayerClass.ROGUE -> R.drawable.hero_03_round
        PlayerClass.PALADIN -> R.drawable.hero_04_round
        PlayerClass.HUNTER -> R.drawable.hero_05_round
        PlayerClass.DRUID -> R.drawable.hero_06_round
        PlayerClass.WARLOCK -> R.drawable.hero_07_round
        PlayerClass.MAGE -> R.drawable.hero_08_round
        PlayerClass.PRIEST -> R.drawable.hero_09_round
        else -> R.drawable.hero_10_round
    }
}

fun sanitizeIndex(classIndex: Int): Int {
    if (classIndex < 0) {
        return allPlayerClasses().size - 1
    } else if (classIndex >= allPlayerClasses().size) {
        return allPlayerClasses().size - 1
    }
    return  classIndex
}

fun getHeroId(classIndex: Int): String {
    return String.format("hero_%02d", sanitizeIndex(classIndex) + 1)
}

object HeroUtil {
    fun getDrawable(playerClass: String): Drawable {
        return Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", getClassIndex(playerClass) + 1))
    }
}