package net.mbonnin.arcanetracker;

import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Typefaces {
    private static Map<String, Typeface> sMap = new HashMap();

    public static Typeface belwe() {
        return get("fonts/belwe-bold.ttf");
    }

    public static Typeface franklin() {
        return get("fonts/franklin-gothic.ttf");
    }

    public static Typeface get(String assetName) {
        Typeface typeface = null;

        if (sMap.get(assetName) != null) {
            return sMap.get(assetName);
        }
        try {
            typeface = Typeface.createFromAsset(ArcaneTrackerApplication.getContext().getAssets(), assetName);
        } catch (Exception e) {
            Timber.e(e);
        }

        if (typeface == null) {
            typeface = Typeface.DEFAULT;
        }
        sMap.put(assetName, typeface);
        return typeface;
    }
}
