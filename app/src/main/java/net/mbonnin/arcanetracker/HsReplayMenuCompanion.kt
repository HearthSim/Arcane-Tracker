package net.mbonnin.arcanetracker

import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.View.GONE
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.hsreplay_menu_view.*
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import net.mbonnin.arcanetracker.ui.main.LoginCompanion
import net.mbonnin.arcanetracker.ui.main.MainActivity
import net.mbonnin.arcanetracker.ui.overlay.Overlay

class HsReplayMenuCompanion(override val containerView: View): LayoutContainer {
    init {
        val isSignedIn = OauthInterceptor.refreshToken != null

        if (isSignedIn) {
            battleTag.setText(HSReplay.get().username())
            battleTag.setOnClickListener {
                ViewManager.get().removeView(containerView)
                Utils.openLink("https://hsreplay.net/account/")
            }
            signout.setOnClickListener {
                Overlay.hide()

                Settings.get(Settings.IS_PRE_HEARTHSIM_USER, false)
                HSReplay.get().unlink()

                val intent = Intent()
                intent.setClass(containerView.context, MainActivity::class.java)
                containerView.context.startActivity(intent)
            }

            if (HSReplay.get().isPremium()) {
                battleTag.setTextColor(Color.parseColor("#FFB00D"))
                premium.visibility = GONE
            } else {
                premium.setOnClickListener {
                    ViewManager.get().removeView(containerView)
                    Utils.openLink("https://hsreplay.net/premium/")
                }
            }
        } else {
            premium.visibility = View.GONE
            signout.visibility = View.GONE

            battleTag.setText(containerView.context.getText(R.string.signIn))
            battleTag.setOnClickListener {
                ViewManager.get().removeView(containerView)
                LoginCompanion.openHsReplayOauth()
            }
        }

        myReplays.setOnClickListener { v3 ->
            ViewManager.get().removeView(containerView)

            Utils.openLink("https://hsreplay.net/games/mine/")
        }

        meta.setOnClickListener { v3 ->
            ViewManager.get().removeView(containerView)

            Utils.openLink("https://hsreplay.net/meta/")
        }

        exploreDecks.setOnClickListener {
            ViewManager.get().removeView(containerView)

            Utils.openLink("https://hsreplay.net/decks/")
        }

    }

    fun oauthSuccess(view: View) {
        ViewManager.get().removeView(view)
    }

    fun oauthCancel(view: View) {
        ViewManager.get().removeView(view)
    }
}
