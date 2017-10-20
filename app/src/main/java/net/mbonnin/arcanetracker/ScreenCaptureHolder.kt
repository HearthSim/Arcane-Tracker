package net.mbonnin.arcanetracker

import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import net.mbonnin.arcanetracker.detector.*
import net.mbonnin.arcanetracker.parser.ArenaParser
import net.mbonnin.arcanetracker.parser.LoadingScreenParser
import net.mbonnin.hsmodel.Card

object ScreenCaptureHolder {
    private val mDetector = Detector(ArcaneTrackerApplication.get(), ArcaneTrackerApplication.get().hasTabletLayout())
    val handler = Handler()
    var screenCaptureStarting = false
    private var screenCapture: ScreenCapture? = null

    val runnable: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun run() {
            if (!screenCaptureStarting && Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true)) {
                if (shouldDetectMode() || shouldDetectArena()) {
                    if (screenCapture == null) {
                        screenCaptureStarting = true
                        val intent = Intent()
                        intent.setClass(ArcaneTrackerApplication.get(), StartScreenCaptureActivity::class.java)
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
            if (shouldDetectMode()) {
                val format = mDetector.detectFormat(bbImage)
                if (format != FORMAT_UNKNOWN) {
                    ScreenCaptureResult.setFormat(format)
                }
                val mode = mDetector.detectMode(bbImage)
                if (mode != MODE_UNKNOWN) {
                    ScreenCaptureResult.setMode(mode)
                    if (mode == MODE_RANKED) {
                        val rank = mDetector.detectRank(bbImage)
                        if (rank != RANK_UNKNOWN) {
                            ScreenCaptureResult.setRank(rank)
                        }
                    }
                }
            }

            if (shouldDetectArena()) {
                val index = DeckList.getArenaDeck().classIndex
                val hero = getPlayerClass(index)
                val arenaResults = mDetector.detectArenaHaar(bbImage, hero)
                ScreenCaptureResult.setArena(arenaResults, hero)
            } else {
                ScreenCaptureResult.clearArena()
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

    fun shouldDetectMode(): Boolean {
        return LoadingScreenParser.MODE_TOURNAMENT == LoadingScreenParser.get().mode
    }

    fun shouldDetectArena(): Boolean {

        val index = DeckList.getArenaDeck().classIndex
        return LoadingScreenParser.MODE_DRAFT == LoadingScreenParser.get().mode
                && ArenaParser.DRAFT_MODE_DRAFTING == ArenaParser.get().draftMode
                && index >= 0
                && index < Card.Companion.CLASS_INDEX_NEUTRAL

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