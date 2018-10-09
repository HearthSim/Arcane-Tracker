package net.mbonnin.arcanetracker.extension

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) VISIBLE else GONE
}