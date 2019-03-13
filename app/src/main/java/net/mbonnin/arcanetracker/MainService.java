package net.mbonnin.arcanetracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import net.mbonnin.arcanetracker.ui.settings.SettingsActivity;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Created by martin on 10/14/16.
 */

public class MainService extends Service {
    private static final int NOTIFICATION_ID = 42;

    public static void stop() {
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(ArcaneTrackerApplication.Companion.getContext(), MainService.class);
        ArcaneTrackerApplication.Companion.getContext().stopService(serviceIntent);
    }

    public static void start() {
        Context context = ArcaneTrackerApplication.Companion.getContext();
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(context, MainService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Utils.INSTANCE.logWithDate("MainService.onCreate()");

        Intent intent = StopServiceBroadcastReceiver.getIntent();
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent(ArcaneTrackerApplication.Companion.getContext(), SettingsActivity.class);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, ArcaneTrackerApplication.Companion.getNOTIFICATION_CHANNEL_ID())
                .setContentText(getString(R.string.arcane_tracker_running))
                .addAction(R.drawable.ic_close_black_24dp, getString(R.string.quit), stopPendingIntent)
                .addAction(R.drawable.ic_settings_black_24dp, getString(R.string.settings), settingsPendingIntent)
                .setSmallIcon(R.drawable.ic_hdt)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Utils.INSTANCE.logWithDate("MainService.onDestroy()");
        FileTree.Companion.get().sync();
        stopForeground(true);
    }
}

