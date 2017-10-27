package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.View.*

fun Activity.makeFullscreen() {
    val decorView = window.decorView
    var uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        uiOptions = uiOptions or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    decorView.systemUiVisibility = uiOptions

    //    decorView.setBackgroundColor(Color.argb(140, 0, 0, 0))
}