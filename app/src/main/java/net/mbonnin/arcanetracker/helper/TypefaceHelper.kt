package net.mbonnin.arcanetracker.helper

import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R

object TypefaceHelper {
    fun belwe(): Typeface? {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.belwe_bold)
    }

    fun franklin(): Typeface? {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.franklin_gothic)
    }
}
