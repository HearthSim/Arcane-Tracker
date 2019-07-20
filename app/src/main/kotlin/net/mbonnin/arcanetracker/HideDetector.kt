package net.mbonnin.arcanetracker

import android.content.Context.WINDOW_SERVICE
import android.os.Build
import android.os.Handler
import android.provider.Settings.canDrawOverlays
import android.util.DisplayMetrics
import android.view.WindowManager
import net.mbonnin.arcanetracker.ui.overlay.Overlay
import net.mbonnin.arcanetracker.ui.settings.SettingsCompanion
import timber.log.Timber


class HideDetector {
    private val mHandler = Handler()
    private var isLandscape = true

    private val mCheckFullScreenRunnable = object : Runnable {

        override fun run() {
            val displayMetrics = DisplayMetrics()
            val windowManager = ArcaneTrackerApplication.get().getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels

            if (height > width && isLandscape) {
                isLandscape = false

                if (Settings.get(Settings.AUTO_HIDE, false)) {
                    Overlay.hide()
                    Timber.d("Hide")
                }
            } else if (height <= width && !isLandscape) {
                isLandscape = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!canDrawOverlays(ArcaneTrackerApplication.get())) {
                        // we might come here from the quit detector. In that case, just do nothing
                        return
                    }
                }

                if (Settings.get(Settings.AUTO_HIDE, false)) {
                    Overlay.show()
                    Timber.d("Show")
                }
            }

            if (!isLandscape) {
                val timeoutMin = SettingsCompanion.getTimeoutValue()

                if (timeoutMin > 0 && System.currentTimeMillis() - lastTimeMillis > timeoutMin * 60 * 1000) {
                    Timber.d("Quit after inactivity")
                    Utils.exitApp()
                }
            }
            mHandler.postDelayed(this, 1000)
        }
    }

    fun start() {
        ping()
        mHandler.postDelayed(mCheckFullScreenRunnable, 4000)
    }

    @Volatile
    private var lastTimeMillis: Long = 0L

    fun ping() {
        lastTimeMillis = System.currentTimeMillis()
    }

    companion object {
        internal var sHideDetector: HideDetector? = null

        fun get(): HideDetector {
            if (sHideDetector == null) {
                sHideDetector = HideDetector()
            }
            return sHideDetector!!
        }
    }
}
