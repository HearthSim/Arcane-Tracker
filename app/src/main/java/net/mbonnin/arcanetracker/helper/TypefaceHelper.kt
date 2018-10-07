package net.mbonnin.arcanetracker.helper

import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import net.mbonnin.arcanetracker.HDTApplication
import net.mbonnin.arcanetracker.R

object TypefaceHelper {
    fun belwe(): Typeface? {
        return ResourcesCompat.getFont(HDTApplication.context, R.font.belwe_bold)
    }

    fun franklin(): Typeface? {
        return ResourcesCompat.getFont(HDTApplication.context, R.font.franklin_gothic)
    }
}
