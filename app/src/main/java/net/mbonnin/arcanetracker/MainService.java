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

/**
 * Created by martin on 10/14/16.
 */

public class MainService extends Service {
    private static final int NOTIFICATION_ID = 42;

    public static void stop() {
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(ArcaneTrackerApplication.getContext(), MainService.class);
        ArcaneTrackerApplication.getContext().stopService(serviceIntent);
    }

    public static void start() {
        Context context = ArcaneTrackerApplication.getContext();
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(context, MainService.class);
        context.startService(serviceIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Utils.logWithDate("MainService.onCreate()");

        Intent intent = StopServiceBroadcastReceiver.getIntent();
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent(ArcaneTrackerApplication.getContext(), SettingsActivity.class);
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

        Utils.logWithDate("MainService.onDestroy()");

        FileTree.get().sync();
        stopForeground(true);
    }
}

