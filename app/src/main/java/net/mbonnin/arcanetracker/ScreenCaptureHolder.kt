package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import net.mbonnin.arcanetracker.detector.Detector
import net.mbonnin.arcanetracker.parser.Entity

object ScreenCaptureHolder {
    @SuppressLint("StaticFieldLeak")
    private val mDetector = Detector(ArcaneTrackerApplication.get(), Utils.is7InchesOrHigher)
    val handler = Handler()
    var screenCaptureStarting = false
    private var screenCapture: ScreenCapture? = null

    val runnable: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun run() {
            if (!screenCaptureStarting) {
                if (shouldDetectRank()
                        && Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true)) {
                    if (screenCapture == null) {
                        screenCaptureStarting = true
                        val intent = Intent()
                        intent.setClass(ArcaneTrackerApplication.get(), StartScreenCaptureActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        ArcaneTrackerApplication.get().startActivity(intent)
                    }
                } else {
                    if (screenCapture != null) {
                        screenCapture?.release()
                        mediaProjection?.stop()
                        screenCapture = null
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }


    val imageConsumer = object : ScreenCapture.Consumer {
        override fun accept(bbImage: ByteBufferImage) {
            mDetector.prepareImage(bbImage)

            if (shouldDetectRank()) {
                val playerRank = mDetector.detectPlayerRank(bbImage)
                val opponentRank = mDetector.detectOpponentRank(bbImage)

                RankHolder.registerRanks(playerRank, opponentRank)
            }
        }
    }

    private var mediaProjection: MediaProjection? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun mediaProjectionCreated(mediaProjection: MediaProjection) {
        screenCaptureStarting = false
        this.mediaProjection = mediaProjection
        screenCapture = ScreenCapture(mediaProjection)
        screenCapture?.addImageConsumer(imageConsumer)
    }

    fun mediaProjectionAborted() {
        screenCaptureStarting = false
        Settings.set(Settings.SCREEN_CAPTURE_ENABLED, false)
    }

    fun shouldDetectRank(): Boolean {
        val game = ArcaneTrackerApplication.get().gameLogicListener.currentGame
        return game != null
                && game.gameEntity!!.tags[Entity.KEY_STEP] == Entity.STEP_BEGIN_MULLIGAN
                && game.gameType == GameType.GT_RANKED.name
    }

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handler.post(runnable)
        }
    }

    fun getScreenCapture(): ScreenCapture? {
        return screenCapture
    }
}