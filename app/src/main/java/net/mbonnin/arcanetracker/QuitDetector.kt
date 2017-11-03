package net.mbonnin.arcanetracker

import android.content.Context.WINDOW_SERVICE
import android.os.Handler
import android.util.DisplayMetrics
import android.view.WindowManager
import timber.log.Timber


class QuitDetector {
    private val mHandler = Handler()
    private var visible = true

    private val mCheckFullScreenRunnable = object : Runnable {

        override fun run() {
            val displayMetrics = DisplayMetrics()
            val windowManager = ArcaneTrackerApplication.get().getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.getDefaultDisplay().getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels

            if (height > width && visible) {
                Overlay.get().hide()
                visible = false

                Timber.d("Hide")

            } else if (height <= width && !visible) {
                Timber.d("Show")

                Overlay.get().show()
                visible = true
            }

            if (!visible) {
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

    @Volatile private var lastTimeMillis: Long = 0L
        private set

    fun ping() {
        lastTimeMillis = System.currentTimeMillis()
    }

    companion object {
        internal var sQuitDetector: QuitDetector? = null

        fun get(): QuitDetector {
            if (sQuitDetector == null) {
                sQuitDetector = QuitDetector()
            }
            return sQuitDetector!!
        }
    }
}
