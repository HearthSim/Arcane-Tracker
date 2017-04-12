package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Created by martin on 11/2/16.
 */

public class SettingsActivity extends Activity {

    @Inject SettingsCompanion settingsCompanion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);
        setContentView(settingsCompanion.settingsView);
    }
}
