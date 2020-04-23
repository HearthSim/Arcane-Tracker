package net.mbonnin.arcanetracker.ui.main

import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.login_view.*
import net.hearthsim.hsreplay.HsReplayOauthApi
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.RandomHelper
import okhttp3.HttpUrl

class LoginCompanion(override val containerView: View) : LayoutContainer {
    private var hasAllPermissions_: Boolean = true
    private var loading_: Boolean = false

    fun loading(loading: Boolean) {
        this.loading_ = loading
        button.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        step.visibility = if (!loading_ && !hasAllPermissions_)  View.INVISIBLE else View.VISIBLE
    }

    fun hasAllPermissions(hasAllPermissions: Boolean) {
        this.hasAllPermissions_ = hasAllPermissions;
        step.visibility = if (!loading_ && !hasAllPermissions_) View.VISIBLE else View.GONE
    }

    init {
        button.setOnClickListener {
            openHsReplayOauth()
        }
    }

    companion object {
        fun openHsReplayOauth() {
            val url = HttpUrl.parse(HsReplayOauthApi.AUTHORIZE_URL)!!
                    .newBuilder()
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("client_id", ArcaneTrackerApplication.oauthParams.clientId)
                    .addQueryParameter("redirect_uri", ArcaneTrackerApplication.oauthParams.clientSecret)
                    .addQueryParameter("scope", "fullaccess")
                    .addQueryParameter("state", RandomHelper.random(16))
            Utils.openLink(url.toString())
        }
    }
}