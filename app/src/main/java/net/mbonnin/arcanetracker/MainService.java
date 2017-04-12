package net.mbonnin.arcanetracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Created by martin on 10/14/16.
 */

public class MainService extends Service {
    private static final int NOTIFICATION_ID = 42;

    @Inject MainViewCompanion mainViewCompanion;
    @Inject QuitDetector quitDetector;
    @Inject ViewManager viewManager;

    public static void stop(Context context) {
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(context, MainService.class);
        context.stopService(serviceIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AndroidInjection.inject(this);

        Utils.logWithDate("MainService.onCreate()");

        mainViewCompanion.setState(MainViewCompanion.STATE_PLAYER, false);
        mainViewCompanion.show(true);
        quitDetector.start();

        File file = new File(Utils.getHearthstoneFilesDir());
        if (!file.exists()) {
            Utils.reportNonFatal(new Exception("cannot find Hearthstone dir"));
            Toast.makeText(this, "could not locate Hearthstone installation directory :-(", Toast.LENGTH_LONG).show();
        }

        Intent intent = StopServiceBroadcastReceiver.getIntent();
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent(this, SettingsActivity.class);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentText(getString(R.string.arcane_tracker_running))
                .addAction(R.drawable.ic_close_black_24dp, "QUIT", stopPendingIntent)
                .addAction(R.drawable.ic_settings_black_24dp, "Settings", settingsPendingIntent)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        quitDetector.stop();
        Utils.logWithDate("MainService.onDestroy()");

        stopForeground(true);
        viewManager.removeAllViews();

        Toast.makeText(this, "Bye bye", Toast.LENGTH_LONG).show();

        System.exit(1);
    }
}

