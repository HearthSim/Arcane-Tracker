package net.mbonnin.arcanetracker

import android.view.View
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor

class HsReplayMenuCompanion(view: View) {
    init {
        val isSignedIn = OauthInterceptor.refreshToken != null
//        view.findViewById<View>(R.id.settings).setOnClickListener { v3 ->
//            ViewManager.get().removeView(view)
//
//            FirebaseAnalytics.getInstance(HDTApplication.context).logEvent("menu_settings", null)
//
//            SettingsCompanion.show()
//        }

    }
}
