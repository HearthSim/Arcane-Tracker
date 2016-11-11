package net.mbonnin.arcanetracker;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.PowerParser;

import java.io.IOException;
import java.util.Locale;

import io.paperdb.Paper;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.util.async.Async;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneTrackerApplication extends Application {
    private static Context sContext;
    private static OkHttpClient sPicassoClient;

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

        /**
         * each image is ~100k and there are ~2000 of them. Put 500 just to be safe :-D
         */
        int cacheSize = 500 * 1024 * 1024;
        sPicassoClient = new OkHttpClient.Builder()
                .cache(new Cache(getCacheDir(), cacheSize))
                .build();

        Picasso picasso = new Picasso.Builder(this)
                .downloader(new OkHttp3Downloader(sPicassoClient))
                .build();
        Picasso.setSingletonInstance(picasso);
        if (Utils.isAppDebuggable()) {
            Picasso.with(this).setIndicatorsEnabled(true);
        }

        StopServiceBroadcastReceiver.init();

        CardDb.init();

        new ArenaParser(Utils.getHearthstoneLogsDir() + "Arena.log", ParserListenerArena.get());
        new LoadingScreenParser(Utils.getHearthstoneLogsDir() + "LoadingScreen.log", ParserListenerLoadingScreen.get());
        new PowerParser(Utils.getHearthstoneLogsDir() + "Power.log", ParserListenerPower.get());

        Parser.get();
    }

    public static void clearPicassoCache() {
        try {
            sPicassoClient.cache().evictAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return sContext;
    }

}
