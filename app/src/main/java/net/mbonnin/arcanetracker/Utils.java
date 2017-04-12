package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.google.firebase.crash.FirebaseCrash;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class Utils {

    public static final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static final MediaType JSON_MIMETYPE = MediaType.parse("application/json; charset=utf-8");

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static Drawable getDrawableForName(Context context, String name) {
        int id = context.getResources().getIdentifier(name.toLowerCase(), "drawable", context.getPackageName());
        if (id > 0) {
            return context.getResources().getDrawable(id);
        } else {
            Timber.e("could not find a bar for id " + name);
            return context.getResources().getDrawable(R.drawable.hero_10);
        }
    }

    public static boolean is7InchesOrHigher(Context context) {
        Display display = ((WindowManager) context.getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float xdpi = context.getResources().getDisplayMetrics().xdpi;
        float ydpi = context.getResources().getDisplayMetrics().ydpi;
        float xinches = outMetrics.heightPixels / xdpi;
        float yinches = outMetrics.widthPixels / ydpi;

        if (Math.hypot(xinches, yinches) > 7) {
            return true;
        }

        return false;
    }

    public static Drawable getDrawableForClassIndex(Context context, int classIndex) {
        return getDrawableForName(context, Card.classIndexToHeroId(classIndex));
    }

    public static boolean isAppDebuggable() {
        return BuildConfig.DEBUG;
    }

    public static void logWithDate(String s) {
        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        Timber.w(df.format(c.getTime()) + s);
    }

    public static void reportNonFatal(Throwable e) {
        Timber.w(e);
        FirebaseCrash.report(e);
    }

    public static int cardMapTotal(Map<String, Integer> map) {
        int total = 0;
        for (String key:map.keySet()) {
            total += cardMapGet(map, key);
        }

        return total;
    }

    public static String getHearthstoneFilesDir() {
        return Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.blizzard.wtcg.hearthstone/files/";
    }

    public static String getHearthstoneLogsDir() {
        return getHearthstoneFilesDir() + "Logs/";
    }

    public static String[] extractMethod(String line) {
        int i = line.indexOf('-');
        if (i < 2 || i >= line.length() - 2) {
            return null;
        }

        if (line.charAt(i -1) != ' ' || line.charAt(i + 1) != ' ') {
            return null;
        }

        String r[] = new String[2];
        r[0] = line.substring(0, i - 1);
        r[1] = line.substring(i + 2);

        return r;
    }

    public static class DummyObserver<T> extends rx.Subscriber<T> {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(T t) {

        }
    }

    public static int cardMapGet(Map<String, Integer> map, String key) {
        Integer a = map.get(key);
        if (a == null) {
            a = 0;
        }
        return a;
    }

    public static void cardMapAdd(Map<String, Integer> map, String key, int diff) {
        int a = cardMapGet(map, key);
        map.put(key, a + diff);
    }
    public static HashMap<String, Integer> cardMapDiff(Map<String, Integer> a, HashMap<String, Integer> b) {
        HashMap<String, Integer> map = new HashMap<>();

        Set<String> set = new HashSet<>(a.keySet());
        set.addAll(b.keySet());

        for (String key: set) {
            Integer an = a.get(key);
            int bn = cardMapGet(b, key);

            if (an == null) {
                Timber.e("key %s is not in a", key);
                continue;
            }

            map.put(key, an - bn);
        }

        return map;
    }

    public static boolean isNetworkConnected(Context context) {
        /**
         * This used to be in ConnectivityReceiver.java so that we don't have to retrieve the service every time but
         * for some reason, it did not work anymore after some time
         */
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}

