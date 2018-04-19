package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import net.mbonnin.arcanetracker.detector.Detector
import net.mbonnin.arcanetracker.parser.ArenaParser
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.LoadingScreenParser
import net.mbonnin.hsmodel.Card

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
                if ((shouldDetectRank() || shouldDetectArena())
                        && Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true)) {
                    if (screenCapture == null) {
                        screenCaptureStarting = true
                        val intent = Intent()
                        intent.setClass(ArcaneTrackerApplication.get(), StartScreenCaptureActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

            if (shouldDetectArena()) {
                val index = LegacyDeckList.arenaDeck.classIndex
                val playerClass = getPlayerClass(index)
                val arenaResults = mDetector.detectArena(bbImage, playerClass)
                ArenaGuessHolder.setArena(arenaResults, playerClass)
            } else {
                ArenaGuessHolder.clear()
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
        val game = GameLogicListener.get().currentGame
        return game != null
                && game.gameEntity!!.tags[Entity.KEY_STEP] == Entity.STEP_BEGIN_MULLIGAN
                && game.gameType == GameType.GT_RANKED.name
    }

    fun shouldDetectArena(): Boolean {
        val index = LegacyDeckList.arenaDeck.classIndex
        return LoadingScreenParser.MODE_DRAFT == LoadingScreenParser.get().mode
                && ArenaParser.DRAFT_MODE_DRAFTING == ArenaParser.get().draftMode
                && index >= 0
                && index < Card.Companion.CLASS_INDEX_NEUTRAL
                && Utils.isAppDebuggable
                && false
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