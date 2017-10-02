package net.mbonnin.arcanetracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.WindowManager;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.LogReader;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.hsmodel.Card;
import net.mbonnin.hsmodel.CardJson;
import net.mbonnin.hsmodel.PlayerClass;

import java.util.ArrayList;
import java.util.Locale;

import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneTrackerApplication extends MultiDexApplication {
    public static final String NOTIFICATION_CHANNEL_ID = "channel_id";
    private static Context sContext;
    private static ArcaneTrackerApplication sArcaneTrackerApplication;
    private LruCache mLruCache;
    private boolean mHasTabletLayout;

    @Override
    public void onCreate() {
        super.onCreate();

        sArcaneTrackerApplication = this;
        sContext = new ContextThemeWrapper(this, R.style.AppThemeLight) {
            @Override
            public void startActivity(Intent intent) {
                if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                    /*
                     * XXX: this is a hack to be able to click textview links
                     */
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                super.startActivity(intent);
            }
        };

        Timber.plant(FileTree.get());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            mChannel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        WindowManager wm = (android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        Point point = new Point();
        display.getRealSize(point);

        double sizeInInches = Math.sqrt((point.x * point.x) / (metrics.xdpi * metrics.xdpi) + (point.y * point.y) / (metrics.ydpi * metrics.ydpi));
        Timber.d("Build.MODEL=" + Build.MODEL);
        Timber.d("Build.MANUFACTURER=" + Build.MANUFACTURER);
        Timber.d("screen size=" + point.x + "x" + point.y);
        Timber.d("sizeInInches=" + sizeInInches);
        mHasTabletLayout = sizeInInches >= 8;

        String langKey = Settings.get(Settings.LANGUAGE, null);
        Timber.d("langKey=" + langKey);
        if (false) {
            /*
             * XXX: this somehow does not work sometimes
             */
            Resources res = sContext.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(langKey);
            res.updateConfiguration(conf, dm);
        }

        Utils.logWithDate("ArcaneTrackerApplication.onCreate() + version=" + BuildConfig.VERSION_CODE);

        Paper.init(this);

        mLruCache = new LruCache(this);

        Picasso picasso = new Picasso.Builder(this)
                .memoryCache(mLruCache)
                .downloader(new OkHttp3Downloader(new OkHttpClient()))
                .addRequestHandler(PicassoCardRequestHandler.get())
                .addRequestHandler(new PicassoBarRequestHandler())
                .build();

        Picasso.setSingletonInstance(picasso);
        if (Utils.isAppDebuggable()) {
            Picasso.with(this).setIndicatorsEnabled(true);
        }

        StopServiceBroadcastReceiver.init();

        initCardJson();

        /*
         * for Arena, we read the whole file again each time because the file is not that big and it allows us to
         * get the arena deck contents
         */
        ArenaParser arenaParser = new ArenaParser();
        new LogReader("Arena.log", arenaParser);

        /*
         * we need to read the whole loading screen if we start Arcane Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        new LogReader("LoadingScreen.log", LoadingScreenParser.get());

        GameLogic.get().addListener(GameLogicListener.get());
        Handler handler = new Handler();
        PowerParser powerParser = new PowerParser(tag -> {
            handler.post(() -> GameLogic.get().handleRootTag(tag));
        }, (gameStr, gameStart) -> {
            GameLogicListener.get().uploadGame(gameStr, gameStart);
            return null;
        });
        /*
         * Power.log, we just want the incremental changes
         */
        new LogReader("Power.log", powerParser, true);

        HSReplay.get();

        CardRenderer.get();

    }

    private void initCardJson() {
        String jsonName = Language.getCurrentLanguage().jsonName;

        ArrayList<Card> injectedCards = new ArrayList<>();

        /*
         * these are 3 fake cards needed for CardRender
         */
        injectedCards.add(CardUtil.INSTANCE.secret(PlayerClass.PALADIN));
        injectedCards.add(CardUtil.INSTANCE.secret(PlayerClass.HUNTER));
        injectedCards.add(CardUtil.INSTANCE.secret(PlayerClass.MAGE));

        CardJson.INSTANCE.init(jsonName, injectedCards);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean hasTabletLayout() {
        return mHasTabletLayout;
    }

    public LruCache getImageCache() {
        return mLruCache;
    }

    public static ArcaneTrackerApplication get() {
        return sArcaneTrackerApplication;
    }

    public static Context getContext() {
        return sContext;
    }

}
