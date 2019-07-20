package net.mbonnin.arcanetracker.helper

import android.graphics.drawable.Drawable
import net.mbonnin.arcanetracker.Utils

object SetHelper {
    fun getDrawable(set: String): Drawable? {
        return Utils.getDrawableForName("ic_set_${set.toLowerCase()}")
    }
}
