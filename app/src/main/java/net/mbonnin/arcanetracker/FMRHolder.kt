package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.Utils.runOnMainThread
import net.mbonnin.arcanetracker.detector.*
import timber.log.Timber
import java.util.*

/**
 * the setters in this class are called from the ScreenCapture thread while the getters
 * are called from the mainThread, hence the volatile
 */
object FMRHolder {
    @Volatile var format = FORMAT_UNKNOWN
        set(value) {
            if (value != field) {
                field = value
                val displayFormat =  if (value == FORMAT_STANDARD) "standard" else "wild"
                displayToast(String.format(Locale.ENGLISH, "format: %s", displayFormat))
                Timber.d("format: " + displayFormat)
            }
        }

    @Volatile var mode = MODE_UNKNOWN
        set(value) {
            if (value != field) {
                field = value
                val displayMode = if (mode == MODE_RANKED) "ranked" else "casual"
                displayToast(String.format(Locale.ENGLISH, "mode: %s", displayMode))
                Timber.d("mode: " + displayMode)
            }
        }

    @Volatile var rank = RANK_UNKNOWN
        set(value) {
            if (value != field) {
                field = value
                displayToast(String.format(Locale.ENGLISH, "rank: %d", value))
                Timber.d("rank: " + value)
            }
        }


    private fun displayToast(toast: String) {
        runOnMainThread({
            Toaster.show(toast)
        })
    }
}
