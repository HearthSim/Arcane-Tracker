package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

import net.mbonnin.arcanetracker.adapter.BarItem;
import net.mbonnin.arcanetracker.parser.CardEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class Utils {
    public static int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                ArcaneTrackerApplication.getContext().getResources().getDisplayMetrics());
    }

    public static Drawable getDrawableForName(String cardId) {
        Context context = ArcaneTrackerApplication.getContext();
        int id = context.getResources().getIdentifier(cardId.toLowerCase(), "drawable", context.getPackageName());
        if (id > 0) {
            return context.getResources().getDrawable(id);
        } else {
            Timber.e("could not find a bar for id " + cardId);
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

        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        Timber.w(df.format(c.getTime()) + s);
    }

    public static void reportNonFatal(Throwable e) {
        Timber.w(e);
        FirebaseCrash.report(e);
    }

    public static int cardMapTotal(HashMap<String, Integer> map) {
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

    public static HashMap<String, Integer> filterCollectibleCards(ArrayList<CardEntity> cards) {
        HashMap<String, Integer> knownCards = new HashMap<>();
        for (CardEntity cardEntity : cards) {
            String cardId = cardEntity.CardID;
            if (TextUtils.isEmpty(cardId)) {
                continue;
            }

            if (!Card.isCollectible(cardId)) {
                continue;
            }


            Utils.cardMapAdd(knownCards, cardId, 1);
        }
        return knownCards;
    }

    public static ArrayList<BarItem> cardMapToBarItems(HashMap<String, Integer> map) {
        ArrayList<BarItem> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: map.entrySet()) {
            BarItem deckEntry = new BarItem();
            deckEntry.card = ArcaneTrackerApplication.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            list.add(deckEntry);
        }

        Collections.sort(list, (a, b) -> {
            int acost = a.card.cost == null ? 0: a.card.cost;
            int bcost = b.card.cost == null ? 0: b.card.cost;

            int ret = acost - bcost;
            if (ret == 0) {
                ret = a.card.name.compareTo(b.card.name);
            }
            return ret;
        });
        return list;
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

    public static boolean isNetworkConnected() {
        /**
         * This used to be in ConnectivityReceiver.java so that we don't have to retrieve the service every time but
         * for some reason, it did not work anymore after some time
         */
        ConnectivityManager cm = (ConnectivityManager) ArcaneTrackerApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}

