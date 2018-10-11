package net.mbonnin.arcanetracker

import android.view.View
import kotlinx.android.extensions.LayoutContainer
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor

class HsReplayMenuCompanion(override val containerView: View?): LayoutContainer {
    init {
        val isSignedIn = OauthInterceptor.refreshToken != null

    }
}
