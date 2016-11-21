package net.mbonnin.arcanetracker;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.multidex.MultiDexApplication;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestHandler;

import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.GameLogic;
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

        new ArenaParser(Utils.getHearthstoneLogsDir() + "Arena.log", new ParserListenerArena());
        new LoadingScreenParser(Utils.getHearthstoneLogsDir() + "LoadingScreen.log", ParserListenerLoadingScreen.get());

        new PowerParser(Utils.getHearthstoneLogsDir() + "Power.log", new ParserListenerPower());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static Context getContext() {
        return sContext;
    }

}
