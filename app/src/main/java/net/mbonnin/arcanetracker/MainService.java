package net.mbonnin.arcanetracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import timber.log.Timber;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ArcaneView.getOpponentAnchorView().minimize(true, false);
        ArcaneView.getPlayerAnchorView().minimize(true, false);

        /*sOpponentAnchorView = (AnchorView) AnchorView.build(this);
        sOpponentAnchorView.init(AnchorView.PREFIX_OPPONENT);*/

        Intent intent = StopServiceBroadcastReceiver.getIntent();
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = SettingsBroadcastReceiver.getIntent();
        PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentText(getString(R.string.arcane_tracker_running))
                .addAction(R.drawable.ic_close_black_24dp, "Stop", stopPendingIntent)
                .addAction(R.drawable.ic_settings_black_24dp, "Settings", settingsPendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");

        stopForeground(true);
        ViewManager.get().removeAllViews();

        Toast.makeText(this, "Bye bye", Toast.LENGTH_LONG).show();
    }
}

