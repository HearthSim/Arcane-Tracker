package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.Utils.runOnMainThread
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import timber.log.Timber
import java.util.*

/**
 * the setters in this class are called from the ScreenCapture thread while the getters
 * are called from the mainThread, hence the volatile
 */
object FMRHolder {
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
