package net.mbonnin.arcanetracker

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import net.hearthsim.analytics.Analytics

class ArcaneTrackerAnalytics : Analytics {
    init {
        /**
         * Firebase has a hard time understanding our metrics as it expects an Activity to be present.
         *
         * Let's try to help a bit...
         */
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).setMinimumSessionDuration(0)
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).setSessionTimeoutDuration(2 * 60 * 60 * 1000)
    }

    override fun logEvent(name: String, params: Map<String, String?>) {
        val bundle = Bundle()

        for (entry in params) {
            bundle.putString(entry.key, entry.value)
        }

        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent(name, bundle)
    }

}