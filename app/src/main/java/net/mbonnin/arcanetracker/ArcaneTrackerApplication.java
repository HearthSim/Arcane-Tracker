package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.LogReader;
import net.mbonnin.arcanetracker.parser.PowerParser;

import java.util.Locale;

import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneTrackerApplication extends MultiDexApplication {
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();

        sContext = new ContextThemeWrapper(this, R.style.AppThemeLight) {
            @Override
            public void startActivity(Intent intent) {
                if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                    /**
                     * XXX: this is a hack to be able to click textview links
                     */
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                super.startActivity(intent);
            }
        };

        Timber.plant(FileTree.get());

        String langKey = Settings.get(Settings.LANGUAGE, null);
        Timber.d("langKey=" + langKey);
        if (false) {
            /**
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

        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(new OkHttpClient()))
                .addRequestHandler(PicassoCardRequestHandler.get())
                .addRequestHandler(new PicassoBarRequestHandler())
                .build();

        Picasso.setSingletonInstance(picasso);
        if (Utils.isAppDebuggable()) {
            Picasso.with(this).setIndicatorsEnabled(true);
        }

        StopServiceBroadcastReceiver.init();

        CardDb.init();

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
        new LogReader("LoadingScreen.log", new LoadingScreenParser(ParserListenerLoadingScreen.get()));

        /*
         * Power.log, we just want the incremental changes
         */
        PowerParser powerParser = new PowerParser();
        new LogReader("Power.log", powerParser, true);

        GameLogic.get().addListener(new GameLogicListener());
        HSReplay.get();

        CardRenderer.get();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static Context getContext() {
        return sContext;
    }

}
