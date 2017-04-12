package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.BroadcastLineConsumer;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.LogReader;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.arcanetracker.parser.RawGameParser;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasDispatchingActivityInjector;
import dagger.android.HasDispatchingServiceInjector;
import internal.di.ObjectGraph;
import internal.di.view.HasDispatchingViewInjector;
import io.paperdb.Paper;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneTrackerApplication extends MultiDexApplication implements HasDispatchingActivityInjector, HasDispatchingViewInjector, HasDispatchingServiceInjector {
    private static Context sContext;

    @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;
    @Inject DispatchingAndroidInjector<View> dispatchingViewInjector;
    @Inject DispatchingAndroidInjector<Service> dispatchingServiceInjector;

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

        ObjectGraph.Initializer.init(this).inject(this);

        Utils.logWithDate("ArcaneTrackerApplication.onCreate() + version=" + BuildConfig.VERSION_CODE);

        StopServiceBroadcastReceiver.start(this);
    }

    @Inject
    void init(Timber.Tree[] trees, Picasso picasso) {
        Timber.plant(trees);

        if (Utils.isAppDebuggable()) {
            picasso.setIndicatorsEnabled(true);
        }
    }

    @Inject
    void initParsers(ArenaParser arenaParser, LoadingScreenParser loadingScreenParser, PowerParser powerParser, RawGameParser rawGameParser) {
        /**
         * for Arena, we read the whole file again each time because the file is not that big and it allows us to
         * get the arena deck contents
         */
        new LogReader(Utils.getHearthstoneLogsDir() + "Arena.log", arenaParser, true);
        /**
         * we need to read the whole loading screen if we start Arcane Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        new LogReader(Utils.getHearthstoneLogsDir() + "LoadingScreen.log", loadingScreenParser, true);

        /**
         * Power.log, we just want the incremental changes
         */
        BroadcastLineConsumer lineConsumer = new BroadcastLineConsumer();
        lineConsumer.add(powerParser);
        lineConsumer.add(rawGameParser);

        new LogReader(Utils.getHearthstoneLogsDir() + "Power.log", lineConsumer, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

//    @Deprecated
//    public static Context getContext() {
//        return sContext;
//    }

    @NonNull
    public static ArcaneTrackerApplication from(@NonNull Context context) {
        return (ArcaneTrackerApplication) context.getApplicationContext();
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingActivityInjector;
    }

    @Override
    public DispatchingAndroidInjector<Service> serviceInjector() {
        return dispatchingServiceInjector;
    }

    @Override
    public DispatchingAndroidInjector<View> viewInjector() {
        return dispatchingViewInjector;
    }
}
