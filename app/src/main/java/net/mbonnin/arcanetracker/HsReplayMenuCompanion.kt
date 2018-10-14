package net.mbonnin.arcanetracker

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.hsreplay_menu_view.*
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import net.mbonnin.arcanetracker.ui.main.MainActivity
import net.mbonnin.arcanetracker.ui.main.OauthCompanion
import net.mbonnin.arcanetracker.ui.my_games.YourGamesActivity
import net.mbonnin.arcanetracker.ui.overlay.Overlay

class HsReplayMenuCompanion(override val containerView: View): LayoutContainer {
    init {
        val isSignedIn = OauthInterceptor.refreshToken != null

        if (isSignedIn) {
            battleTag.setText(HSReplay.get().username())

            signout.setOnClickListener {
                Overlay.hide()

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
                    Utils.openLink("https://hsreplay.net/premium/")
                }
            }
        } else {
            premium.visibility = View.GONE
            signout.visibility = View.GONE

            battleTag.setText(containerView.context.getText(R.string.signIn))
            battleTag.setOnClickListener {
                val view = LayoutInflater.from(containerView.context).inflate(R.layout.oauth_view, null, false)
                val companion = OauthCompanion(view, this::oauthSuccess, this::oauthCancel)

                companion.setWebViewMargin(Utils.dpToPx(30))
                val params = ViewManager.Params()
                params.x = 0
                params.y = 0
                params.w = ViewManager.get().width
                params.h = ViewManager.get().height

                ViewManager.get().addView(view, params)
            }
        }

        myReplays.setOnClickListener { v3 ->
            ViewManager.get().removeView(containerView)

            FirebaseAnalytics.getInstance(HDTApplication.context).logEvent("menu_history", null)

            val intent = Intent()
            intent.setClass(HDTApplication.context, YourGamesActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            HDTApplication.context.startActivity(intent)
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
