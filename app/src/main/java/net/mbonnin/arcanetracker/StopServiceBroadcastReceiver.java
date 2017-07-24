package net.mbonnin.arcanetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.esotericsoftware.kryo.util.Util;

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

    public static void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);
        ArcaneTrackerApplication.getContext().registerReceiver(new StopServiceBroadcastReceiver(), filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.exitApp();
    }
}
