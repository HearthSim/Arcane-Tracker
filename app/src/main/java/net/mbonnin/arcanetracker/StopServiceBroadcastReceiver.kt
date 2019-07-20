package net.mbonnin.arcanetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Created by martin on 10/24/16.
 */

class StopServiceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Utils.exitApp()
    }

    companion object {
        val ACTION = "net.mbonnin.arcanetracker.StopServiceBroadcastReceiver"

        val intent: Intent
            get() {
                val intent = Intent()
                intent.action = ACTION

                return intent
            }

        fun init() {
            val filter = IntentFilter()
            filter.addAction(ACTION)
            ArcaneTrackerApplication.context.registerReceiver(StopServiceBroadcastReceiver(), filter)
        }
    }
}
