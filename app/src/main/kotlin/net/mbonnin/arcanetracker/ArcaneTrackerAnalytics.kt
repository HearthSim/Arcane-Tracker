package net.mbonnin.arcanetracker

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import net.hearthsim.analytics.Analytics

class ArcaneTrackerAnalytics: Analytics {
    override fun logEvent(name: String, params: Map<String, String?>) {
        val bundle = Bundle()

        for (entry in params) {
            bundle.putString(entry.key, entry.value)
        }

        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("game_ended", bundle)
    }

}