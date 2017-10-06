package net.mbonnin.arcanetracker;

import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Typefaces {
    public static Typeface belwe() {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.getContext(), R.font.belwe_bold);
    }

    public static Typeface franklin() {
        return ResourcesCompat.getFont(ArcaneTrackerApplication.getContext(), R.font.franklin_gothic);
    }
}
