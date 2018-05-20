package net.mbonnin.arcanetracker

import android.graphics.drawable.Drawable

object SetHelper {
    fun getDrawable(set: String): Drawable? {
        return Utils.getDrawableForName("ic_set_${set.toLowerCase()}")
    }
}
