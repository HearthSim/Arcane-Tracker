package net.mbonnin.arcanetracker

import android.os.Handler
import android.widget.Toast

import net.mbonnin.arcanetracker.detector.*

import java.util.Locale

import rx.Completable
import rx.android.schedulers.AndroidSchedulers

import net.mbonnin.arcanetracker.detector.FORMAT_UNKNOWN
import net.mbonnin.arcanetracker.detector.MODE_UNKNOWN
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import rx.functions.Action0

object ScreenCaptureResult {
    @Volatile private var rank = RANK_UNKNOWN
    @Volatile private var format = FORMAT_UNKNOWN
    @Volatile private var mode = MODE_UNKNOWN

    private val handler = Handler()

    class LowPass {
        var count = 0
        var cardId = ""
        var reset = Runnable {}
    }

    private val filters = Array(3, {LowPass()})

    fun setRank(rank: Int) {
        if (rank != ScreenCaptureResult.rank) {
            ScreenCaptureResult.rank = rank
            displayToast(String.format(Locale.ENGLISH, "rank: %d", rank))
        }
    }

    private fun runOnMainThread(action: Action0) {
        Completable.fromAction(action)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
    private fun displayToast(toast: String) {
        runOnMainThread(Action0 { Toast.makeText(ArcaneTrackerApplication.getContext(), toast, Toast.LENGTH_SHORT).show() })
    }

    fun setFormat(format: Int) {
        if (format != ScreenCaptureResult.format) {
            ScreenCaptureResult.format = format
            displayToast(String.format(Locale.ENGLISH, "format: %s", if (format == FORMAT_STANDARD) "standard" else "wild"))
        }
    }

    fun setMode(mode: Int) {
        if (mode != ScreenCaptureResult.mode) {
            ScreenCaptureResult.mode = mode
            displayToast(String.format(Locale.ENGLISH, "mode: %s", if (mode == MODE_RANKED) "ranked" else "casual"))
        }
    }

    fun getRank(): Int {
        return rank
    }

    fun getMode(): Int {
        return mode
    }

    fun getFormat(): Int {
        return format
    }

    fun reset() {
        rank = RANK_UNKNOWN
        mode = MODE_UNKNOWN
        format = FORMAT_UNKNOWN
    }

    fun setArena(arenaResult: Array<String>) {
        for (i in 0..2) {
            if (arenaResult[i] != filters[i].cardId) {

            }
        }
    }
}
