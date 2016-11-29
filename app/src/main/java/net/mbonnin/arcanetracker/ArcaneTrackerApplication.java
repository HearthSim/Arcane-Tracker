package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.multidex.MultiDexApplication;
import android.view.ContextThemeWrapper;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.BroadcastLineConsumer;
import net.mbonnin.arcanetracker.parser.RawGameParser;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.LogReader;
import net.mbonnin.arcanetracker.parser.PowerParser;

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

        Timber.plant(new Timber.DebugTree());
        Timber.plant(FileTree.get());

        Utils.logWithDate("ArcaneTrackerApplication.onCreate() + version=" + BuildConfig.VERSION_CODE);

        Paper.init(this);

        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(new OkHttpClient()))
                .addRequestHandler(PicassoCardRequestHandler.get())
                .build();

        Picasso.setSingletonInstance(picasso);
        if (Utils.isAppDebuggable()) {
            Picasso.with(this).setIndicatorsEnabled(true);
        }

        StopServiceBroadcastReceiver.init();

        CardDb.init();

        boolean readPreviousData;
        /**
         * for Arena, we read the whole file again each time because the file is not that big and it allows us to
         * get the arena deck contents
         */
        ArenaParser arenaParser = new ArenaParser(new ParserListenerArena());
        readPreviousData = true;
        new LogReader(Utils.getHearthstoneLogsDir() + "Arena.log", arenaParser, readPreviousData);
        /**
         * we need to read the whole loading screen if we start Arcane Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        readPreviousData = true;
        new LogReader(Utils.getHearthstoneLogsDir() + "LoadingScreen.log", new LoadingScreenParser(ParserListenerLoadingScreen.get()), readPreviousData);

        /**
         * Power.log, we just want the incremental changes
         */
        readPreviousData = false;
        PowerParser powerParser = new PowerParser(ParserListenerPower.get());
        RawGameParser rawGameParser = new RawGameParser();
        BroadcastLineConsumer lineConsumer = new BroadcastLineConsumer();
        lineConsumer.add(powerParser);
        lineConsumer.add(rawGameParser);

        new LogReader(Utils.getHearthstoneLogsDir() + "Power.log", lineConsumer, readPreviousData);

        HSReplay.get();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static Context getContext() {
        return sContext;
    }

}
