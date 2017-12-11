package net.mbonnin.arcanetracker

import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat

object Typefaces {
    fun belwe(): Typeface? {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.belwe_bold)
    }

    fun franklin(): Typeface? {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.franklin_gothic)
    }
}
