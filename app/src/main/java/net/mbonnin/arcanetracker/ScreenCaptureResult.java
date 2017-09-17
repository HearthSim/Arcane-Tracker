package net.mbonnin.arcanetracker;

import android.widget.Toast;

import net.mbonnin.arcanetracker.detector.DetectorKt;

import java.util.Locale;

import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;

class ScreenCaptureResult {
    private static volatile int rank = DetectorKt.RANK_UNKNOWN;
    private static volatile int format = DetectorKt.FORMAT_UNKNOWN;
    private static volatile int mode = DetectorKt.MODE_UNKNOWN;


    public static void setRank(int rank) {
        if (rank != ScreenCaptureResult.rank) {
            ScreenCaptureResult.rank = rank;
            displayToast(String.format(Locale.ENGLISH, "rank: %d", rank));
        }
    }

    private static void displayToast(String toast) {
        Completable.fromAction(() -> {
            Toast.makeText(ArcaneTrackerApplication.getContext(), toast, Toast.LENGTH_SHORT).show();
        }).subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }


    public static void setFormat(int format) {
        if (format != ScreenCaptureResult.format) {
            ScreenCaptureResult.format = format;
            displayToast(String.format(Locale.ENGLISH, "format: %s", format == DetectorKt.FORMAT_STANDARD ? "standard": "wild"));
        }
    }

    public static void setMode(int mode) {
        if (mode != ScreenCaptureResult.mode) {
            ScreenCaptureResult.mode = mode;
            displayToast(String.format(Locale.ENGLISH, "mode: %s", mode == DetectorKt.MODE_RANKED ? "ranked": "casual"));
        }
    }

    public static int getRank() {
        return rank;
    }

    public static int getMode() {
        return mode;
    }

    public static int getFormat() {
        return format;
    }
}
