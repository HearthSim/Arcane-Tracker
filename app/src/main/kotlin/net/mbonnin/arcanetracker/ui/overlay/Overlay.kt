package net.mbonnin.arcanetracker.ui.overlay

import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hearthsim.hslog.PossibleSecret
import net.hearthsim.hsmodel.enum.CardId
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.arcanetracker.ui.overlay.view.TopDrawerCompanion
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

        setAlphaProgress(getAlphaProgress())
        setButtonWidth(getButtonWidth())

        /*val possibleSecrets = listOf(
                PossibleSecret(CardId.COUNTERSPELL, 1),
                PossibleSecret(CardId.SPELLBENDER, 1),
                PossibleSecret(CardId.ICE_BARRIER, 0),
                PossibleSecret(CardId.DUPLICATE, 1),
                PossibleSecret(CardId.MIRROR_ENTITY, 1),
                PossibleSecret(CardId.EFFIGY, 0)
        )

        GlobalScope.launch(Dispatchers.Main) {
            delay(4000)
            var i = 3
            while (true) {
                if (i % 5 == 0) {
                    MainViewCompanion.get().onSecrets(
                            emptyList()
                    )

                } else {
                    MainViewCompanion.get().onSecrets(
                            possibleSecrets.shuffled()
                    )
                }
                delay(2000)
                i++
            }
        }*/
    }

    fun setAlphaProgress(progress: Int) {
        Settings.set(Settings.ALPHA, progress)
        MainViewCompanion.get().setAlpha(progress)
    }

    fun getAlphaProgress(): Int {
        return Settings.get(Settings.ALPHA, 100)
    }

    fun setButtonWidth(buttonWidth: Int) {
        Settings.set(Settings.BUTTON_WIDTH, buttonWidth - Utils.dpToPx(8))
        MainViewCompanion.get().setButtonWidth(buttonWidth)
    }

    fun getButtonWidth(): Int {
        var w = Settings.get(Settings.BUTTON_WIDTH, 0) + Utils.dpToPx(8) // when adding the tutorial, I made the button slightly smaller than what they used to be
        if (w < minButtonWidth || w >= maxButtonWidth) {
            val dp = if (Utils.is7InchesOrHigher) 50 else 38
            w = Utils.dpToPx(dp)
        }

        return w
    }

    val maxButtonWidth = Utils.dpToPx(75)

    val minButtonWidth = Utils.dpToPx(20)

    fun hide() {
        ViewManager.get().removeAllViews()
    }
}
