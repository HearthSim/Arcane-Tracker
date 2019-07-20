package net.mbonnin.arcanetracker.ui.overlay

import android.view.LayoutInflater
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.arcanetracker.ui.overlay.view.WhatsNewCompanion

object Overlay {

    fun show() {
        MainViewCompanion.get().setState(MainViewCompanion.STATE_PLAYER, false)
        MainViewCompanion.get().show(true)

        val context = ArcaneTrackerApplication.context

        val previousVersion = Settings[Settings.VERSION, 0]
        Settings[Settings.VERSION] = BuildConfig.VERSION_CODE

        if (Settings[Settings.SHOW_CHANGELOG, true]
                && previousVersion > 0
                && previousVersion < BuildConfig.VERSION_CODE) {
            val view = LayoutInflater.from(context).inflate(R.layout.whats_new, null)
            WhatsNewCompanion(view, previousVersion)
        }

        Onboarding.start()
    }

    fun hide() {
        ViewManager.get().removeAllViews()
    }
}
