package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.MediaType;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class Utils {

    public static final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
    public static final MediaType JSON_MIMETYPE = MediaType.parse("application/json; charset=utf-8");

    public static int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                ArcaneTrackerApplication.getContext().getResources().getDisplayMetrics());
    }

    public static Drawable getDrawableForName(String name) {
        Context context = ArcaneTrackerApplication.getContext();
        int id = context.getResources().getIdentifier(name.toLowerCase(), "drawable", context.getPackageName());
        if (id > 0) {
            return context.getResources().getDrawable(id);
        } else {
            Timber.e("could not find a bar for id " + name);
            return context.getResources().getDrawable(R.drawable.hero_10);
        }
    }

    public static boolean is7InchesOrHigher() {
        Context context = ArcaneTrackerApplication.getContext();
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

    public static Drawable getDrawableForClassIndex(int classIndex) {
        return getDrawableForName(Card.classIndexToHeroId(classIndex));
    }

    public static boolean isAppDebuggable() {
        Context context = ArcaneTrackerApplication.getContext();
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static void logWithDate(String s) {
        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss ", Locale.ENGLISH);

        Timber.w(df.format(c.getTime()) + s);
    }

    public static void reportNonFatal(Throwable e) {
        Timber.w(e);
        FirebaseCrash.report(e);
    }

    public static int cardMapTotal(HashMap<String, Integer> map) {
        int total = 0;
        for (String key : map.keySet()) {
            total += cardMapGet(map, key);
        }

        return total;
    }

    public static String getHSExternalDir() {
        return Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.blizzard.wtcg.hearthstone/files/";
    }

    public static String getHSInternalDir() {
        return "/data/data/com.blizzard.wtcg.hearthstone/files";
    }

    public static Bitmap getAssetBitmap(String name) {
        Context context = ArcaneTrackerApplication.getContext();
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(name);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Bitmap b = BitmapFactory.decodeStream(inputStream);
        return b;
    }

    public static Bitmap getAssetBitmap(String name, String defaultName) {
        Bitmap b = getAssetBitmap(name);
        if (b == null) {
            return getAssetBitmap(defaultName);
        } else {
            return b;
        }
    }

    public static String getCardUrl(String id) {
        return "card://" + Language.getCurrentLanguage().key + "/" + id;
    }

    public static double getDiagonal() {
        WindowManager wm = (WindowManager) ArcaneTrackerApplication.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int w = size.x;
        int h = size.y;
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(w / dm.xdpi, 2);
        double y = Math.pow(h / dm.ydpi, 2);

        return Math.sqrt(x + y);
    }

    public static void exitApp() {
        Overlay.get().hide();
        FileTree.get().sync();
        System.exit(0);
    }

    public static void openLink(String url) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setData(Uri.parse(url));
        ArcaneTrackerApplication.getContext().startActivity(i);
    }

    public static int valueOf(Integer i) {
        return i == null ? 0 : i;
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

    public static int cardMapGet(HashMap<String, Integer> map, String key) {
        Integer a = map.get(key);
        if (a == null) {
            a = 0;
        }
        return a;
    }

    public static void cardMapAdd(HashMap<String, Integer> map, String key, int diff) {
        int a = cardMapGet(map, key);
        map.put(key, a + diff);
    }

    public static HashMap<String, Integer> cardMapDiff(HashMap<String, Integer> a, HashMap<String, Integer> b) {
        HashMap<String, Integer> map = new HashMap<>();

        Set<String> set = new HashSet<>(a.keySet());
        set.addAll(b.keySet());

        for (String key : set) {
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

    public static boolean isNetworkConnected() {
        /**
         * This used to be in ConnectivityReceiver.java so that we don't have to retrieve the service every time but
         * for some reason, it did not work anymore after some time
         */
        ConnectivityManager cm = (ConnectivityManager) ArcaneTrackerApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, "UTF-8");
        for (; ; ) {
            int rsz = in.read(buffer, 0, buffer.length);
            if (rsz < 0)
                break;
            out.append(buffer, 0, rsz);
        }
        return out.toString();
    }
}

