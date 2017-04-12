package net.mbonnin.arcanetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by martin on 10/24/16.
 */

public class StopServiceBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "net.mbonnin.arcanetracker.StopServiceBroadcastReceiver";

    public static Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction(ACTION);

        return intent;
    }

    public static void start(Context context) {
        context.registerReceiver(new StopServiceBroadcastReceiver(), new IntentFilter(ACTION));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MainService.stop(context);
    }
}
