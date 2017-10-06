package net.mbonnin.arcanetracker

import android.widget.Toast

import net.mbonnin.arcanetracker.detector.*

import java.util.Locale

import rx.Completable
import rx.android.schedulers.AndroidSchedulers

import net.mbonnin.arcanetracker.detector.FORMAT_UNKNOWN
import net.mbonnin.arcanetracker.detector.MODE_UNKNOWN
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN

object ScreenCaptureResult {
    @Volatile private var rank = RANK_UNKNOWN
    @Volatile private var format = FORMAT_UNKNOWN
    @Volatile private var mode = MODE_UNKNOWN


    fun setRank(rank: Int) {
        if (rank != ScreenCaptureResult.rank) {
            ScreenCaptureResult.rank = rank
            displayToast(String.format(Locale.ENGLISH, "rank: %d", rank))
        }
    }

    private fun displayToast(toast: String) {
        Completable.fromAction { Toast.makeText(ArcaneTrackerApplication.getContext(), toast, Toast.LENGTH_SHORT).show() }.subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
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
}
