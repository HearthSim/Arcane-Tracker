package net.mbonnin.arcanetracker.ui.overlay

import android.annotation.SuppressLint
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.Button
import android.widget.TextView
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.ui.overlay.view.HandleView
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion

@SuppressLint("StaticFieldLeak")
object Onboarding {
    var playerPopup: View? = null
    var opponentPopup: View? = null
    var hsReplayPopup: View? = null

    val handler = Handler()

    fun start() {
        if (Settings.get(Settings.ONBOARDING_FINISHED, false)) {
            return
        }

        handler.postDelayed ({
            playerPopup?.let { ViewManager.get().removeView(it) }
            playerPopup = displayPopup(getHandleView(R.id.playerHandle), R.string.onoboarding_player)
        }, 300)
    }

    fun displayPopup(handleView: HandleView, textResId: Int): View {
        val view = LayoutInflater.from(HDTApplication.get()).inflate(R.layout.onboarding_view, null, false)
        view.findViewById<TextView>(R.id.text).setText(Utils.getString(textResId))
        view.findViewById<Button>(R.id.button).visibility = GONE


        view.findViewById<View>(R.id.button).setOnClickListener {
            handleView.glow(false)
        }
        handleView.glow(true)

        val outLocation = IntArray(2)
        handleView.getLocationOnScreen(outLocation)
        ViewManager.get().addViewWithAnchor(view, outLocation[0] + handleView.width, outLocation[1])

        handleView.glow(true)

        return view
    }

    fun getHandleView(resId: Int): HandleView {
        return MainViewCompanion.get().handlesView.findViewById<HandleView>(resId)
    }

    fun playerHandleClicked() {
        if (playerPopup == null) {
            return
        }

        playerPopup?.let { ViewManager.get().removeView(it) }
        playerPopup = null
        getHandleView(R.id.playerHandle).glow(false)

        handler.postDelayed ({
            opponentPopup?.let { ViewManager.get().removeView(it) }
            opponentPopup = displayPopup(getHandleView(R.id.opponentHandle), R.string.onboarding_opponent)
        }, 300)
    }

    fun opponentHandleClicked() {
        if (opponentPopup == null) {
            return
        }

        opponentPopup?.let { ViewManager.get().removeView(it) }
        opponentPopup = null
        getHandleView(R.id.opponentHandle).glow(false)

        handler.postDelayed ({
            hsReplayPopup?.let { ViewManager.get().removeView(it) }
            hsReplayPopup = displayPopup(getHandleView(R.id.hsReplayHandle), R.string.onboarding_hsReplay)
        }, 300)
    }

    fun hsReplayHandleClicked() {
        if (hsReplayPopup == null) {
            return
        }

        hsReplayPopup?.let { ViewManager.get().removeView(it) }
        hsReplayPopup = null
        getHandleView(R.id.hsReplayHandle).glow(false)

        finishOnboarding()
    }

    private fun finishOnboarding() {

        Settings.set(Settings.ONBOARDING_FINISHED, true)
    }

    fun updateTranslation() {
        playerPopup?.let { updateX(it, R.id.playerHandle) }
        opponentPopup?.let { updateX(it, R.id.opponentHandle) }
    }

    private fun updateX(view: View, handleResId: Int) {
        val handleView = getHandleView(handleResId)

        val params = ViewManager.Params()
        val outLocation = IntArray(2)
        handleView.getLocationOnScreen(outLocation)

        params.w = view.measuredWidth
        params.h = view.measuredHeight
        
        params.x = outLocation[0] + handleView.getWidth() + Utils.dpToPx(20)
        params.y = (outLocation[1] - params.h / 2)
        if (params.y < 0) {
            params.y = 0
        } else if (params.y + params.h > ViewManager.get().height) {
            params.y = ViewManager.get().height - params.h
        }


        ViewManager.get().updateView(view, params)
    }
}