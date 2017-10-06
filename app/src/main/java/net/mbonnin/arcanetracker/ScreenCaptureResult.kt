package net.mbonnin.arcanetracker

import android.widget.Toast
import net.mbonnin.arcanetracker.detector.*
import rx.Completable
import rx.android.schedulers.AndroidSchedulers
import java.util.*

object ScreenCaptureResult {
    @Volatile private var rank = RANK_UNKNOWN
    @Volatile private var format = FORMAT_UNKNOWN
    @Volatile private var mode = MODE_UNKNOWN

    class LowPass {
        var count = 0
        var cardId = ""
    }

    private val filters = Array(3, {LowPass()})

    fun setRank(rank: Int) {
        if (rank != ScreenCaptureResult.rank) {
            ScreenCaptureResult.rank = rank
            displayToast(String.format(Locale.ENGLISH, "rank: %d", rank))
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        Completable.fromAction(action)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
    private fun displayToast(toast: String) {
        runOnMainThread({ Toast.makeText(ArcaneTrackerApplication.getContext(), toast, Toast.LENGTH_SHORT).show() })
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

    fun setArena(arenaResult: Array<String>, hero: String) {

        for (i in 0..2) {
            if (arenaResult[i] != filters[i].cardId) {
                filters[i].count = 0
                filters[i].cardId = arenaResult[i]
                runOnMainThread({ArenaGuessCompanion.hide(i)})
            } else {
                filters[i].count++
                if (filters[i].count == 50) {
                    runOnMainThread({ArenaGuessCompanion.show(i, filters[i].cardId, hero)})
                }
            }
        }
    }

    fun clearArena() {
        for (i in 0..2) {
            if ("" != filters[i].cardId) {
                runOnMainThread { ArenaGuessCompanion.hide(i)}
                filters[i].cardId = ""
            }
        }

    }
}
