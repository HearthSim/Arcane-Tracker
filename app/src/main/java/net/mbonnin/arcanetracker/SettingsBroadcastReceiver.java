package net.mbonnin.arcanetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by martin on 10/28/16.
 */
public class SettingsBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "net.mbonnin.arcanetracker.SettingsBroadcastReceiver";

    public static Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction(ACTION);

        return intent;
    }

    public static void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);
        ArcaneTrackerApplication.getContext().registerReceiver(new SettingsBroadcastReceiver(), filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SettingsViewHolder.show();
    }}
