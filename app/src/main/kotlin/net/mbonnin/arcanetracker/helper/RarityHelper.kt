package net.mbonnin.arcanetracker.helper

import android.graphics.Color
import net.hearthsim.hsmodel.enum.Rarity

object RarityHelper {
    val rarityToColor = mapOf(
            Rarity.COMMON to Color.WHITE,
            Rarity.RARE to Color.rgb(49, 134, 222),
            Rarity.EPIC to Color.rgb(173, 113, 247),
            Rarity.LEGENDARY to Color.rgb(255, 154, 16)
    )
    val GOLD_COLOR = Color.argb(255, 0xff, 0xd5, 0x4f)
}