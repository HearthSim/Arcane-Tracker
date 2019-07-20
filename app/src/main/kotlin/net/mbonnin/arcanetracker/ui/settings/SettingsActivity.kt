package net.mbonnin.arcanetracker.ui.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View

import net.mbonnin.arcanetracker.R

/**
 * Created by martin on 11/2/16.
 */

class SettingsActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this).inflate(R.layout.settings_view, null)
        SettingsCompanion(view)
        setContentView(view)
    }
}
