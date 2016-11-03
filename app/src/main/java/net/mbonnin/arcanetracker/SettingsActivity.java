package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by martin on 11/2/16.
 */

public class SettingsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.settings_view, null);
        new SettingsCompanion(view);
        setContentView(view);
    }
}
