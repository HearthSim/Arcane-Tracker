package net.mbonnin.arcanetracker.ui.main

import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.login_view.*
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.RandomHelper
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import okhttp3.HttpUrl

class LoginCompanion(override val containerView: View): LayoutContainer {
    fun loading(loading: Boolean) {
        button.visibility = if (loading) View.GONE else View.VISIBLE
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    init {
        button.setOnClickListener {
            val url = HttpUrl.parse(OauthInterceptor.AUTHORIZE_URL)!!
                    .newBuilder()
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("client_id", OauthInterceptor.A)
                    .addQueryParameter("redirect_uri", OauthInterceptor.CALLBACK_URL)
                    .addQueryParameter("scope", "fullaccess")
                    .addQueryParameter("state", RandomHelper.random(16))
            Utils.openLink(url.toString())
        }
    }
}