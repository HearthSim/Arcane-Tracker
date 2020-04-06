package net.mbonnin.arcanetracker

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.mbonnin.arcanetracker.ui.settings.SettingsActivity

/**
 * Created by martin on 10/14/16.
 */

class MainService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Utils.logWithDate("MainService.onCreate()")

        val intent = StopServiceBroadcastReceiver.intent
        val stopPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val intent2 = Intent(ArcaneTrackerApplication.context, SettingsActivity::class.java)
        val settingsPendingIntent = PendingIntent.getActivity(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, ArcaneTrackerApplication.NOTIFICATION_CHANNEL_ID)
                .setContentText(getString(R.string.arcane_tracker_running))
                .addAction(R.drawable.ic_close_black_24dp, getString(R.string.quit), stopPendingIntent)
                .addAction(R.drawable.ic_settings_black_24dp, getString(R.string.settings), settingsPendingIntent)
                .setSmallIcon(R.drawable.ic_hdt)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }


    override fun onDestroy() {
        super.onDestroy()

        Utils.logWithDate("MainService.onDestroy()")
        FileTree.get().sync()
        stopForeground(true)
    }

    companion object {
        private val NOTIFICATION_ID = 42

        fun stop() {
            val serviceIntent = Intent()
            serviceIntent.setClass(ArcaneTrackerApplication.context, MainService::class.java)
            ArcaneTrackerApplication.context.stopService(serviceIntent)
        }

        fun start() {
            val context = ArcaneTrackerApplication.context
            val serviceIntent = Intent()
            serviceIntent.setClass(context, MainService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}

