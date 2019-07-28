package net.mbonnin.arcanetracker.ui.overlay.view

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.ui.my_games.YourGamesActivity
import net.mbonnin.arcanetracker.ui.my_packs.YourPacksActivity
import net.mbonnin.arcanetracker.ui.overlay.Onboarding
import net.mbonnin.arcanetracker.ui.overlay.Onboarding.hsReplayHandleClicked
import net.mbonnin.arcanetracker.ui.settings.SettingsCompanion
import net.mbonnin.arcanetracker.ui.stats.YourDecksActivity

class MainViewCompanion(val mainView: View) {
    private val mHandler: Handler
    private val frameLayout: View

    private val playerView: View
    private val opponentView: View
    private val mViewManager = ViewManager.get()

    val handlesView = LayoutInflater.from(mainView.context).inflate(R.layout.handles_view, null) as HandlesView


    private var state: Int = 0

    private var mWidth = 0

    private val drawerHelper = DrawerHelper(mainView, handlesView, DrawerHelper.Edge.LEFT)
    fun setAlpha(progress: Int) {
        drawerHelper.setAlpha(progress)
    }

    init {
        mHandler = Handler()

        frameLayout = mainView.findViewById(R.id.frameLayout)
        opponentView = mainView.findViewById(R.id.opponentView)
        playerView = mainView.findViewById(R.id.playerView)

        mWidth = Settings.get(Settings.DRAWER_WIDTH, 0)
        if (mWidth < minDrawerWidth || mWidth >= maxDrawerWidth) {
            mWidth = (0.33 * 0.5 * mViewManager.width.toDouble()).toInt()
        }
        setDrawerWidth(mWidth)

        sOpponentCompanion = OpponentDeckCompanion(opponentView)
        sPlayerCompanion = PlayerDeckCompanion(playerView)

        drawerHelper.setViewHeight(mViewManager.height)

        configureHandles(handlesView)

        setState(STATE_PLAYER, false)
    }

    val minDrawerWidth: Int
        get() = Utils.dpToPx(50)

    val maxDrawerWidth: Int
        get() = (0.4 * mViewManager.width).toInt()

    fun setDrawerWidth(width: Int) {
        mWidth = width
        Settings.set(Settings.DRAWER_WIDTH, width)

        drawerHelper.setViewWidth(width)
    }

    fun getDrawerWidth(): Int {
        return mWidth
    }

    fun setButtonWidth(buttonWidth: Int) {
        drawerHelper.setButtonWidth(buttonWidth)
    }

    internal inner class ClickListener(private val targetState: Int) : View.OnClickListener {

        override fun onClick(v: View) {
            if (state == targetState && drawerHelper.isOpen()) {
                setState(state, false)
            } else {
                setState(targetState, true)
            }
        }
    }

    fun setState(targetState: Int, drawerOpen: Boolean) {
        if (drawerOpen) {
            opponentView.visibility = View.GONE
            playerView.visibility = View.GONE
            when (targetState) {
                STATE_PLAYER -> {
                    playerView.visibility = View.VISIBLE
                    ArcaneTrackerApplication.get().analytics.logEvent("state_player")

                    Onboarding.playerHandleClicked()
                }
                STATE_OPPONENT -> {
                    opponentView.visibility = View.VISIBLE
                    ArcaneTrackerApplication.get().analytics.logEvent("state_opponent")

                    Onboarding.opponentHandleClicked()
                }
            }
            drawerHelper.open()
        } else {
            drawerHelper.close()
        }

        state = targetState
    }


    fun show(show: Boolean) {
        drawerHelper.show(show)
    }

    private fun configureHandles(v: View) {
        var handleView: HandleView
        var drawable: Drawable

        handleView = v.findViewById(R.id.hsReplayHandle)
        drawable = v.context.resources.getDrawable(R.drawable.ic_hs_replay)
        handleView.init(drawable, v.context.resources.getColor(R.color.hsReplayBlue))
        handleView.setOnClickListener {
            val view = LayoutInflater.from(v.context).inflate(R.layout.hsreplay_menu_view, null)
            HsReplayMenuCompanion(view)
            hsReplayHandleClicked()
            mViewManager.addMenu(view, it)
        }

        handleView = v.findViewById(R.id.settingsHandle)
        drawable = v.context.resources.getDrawable(R.drawable.settings_handle)
        handleView.init(drawable, v.context.resources.getColor(R.color.gray))
        handleView.setOnClickListener { v2 ->
            val view = LayoutInflater.from(v.context).inflate(R.layout.cog_menu, null)

            view.findViewById<View>(R.id.settings).setOnClickListener {
                mViewManager.removeView(view)

                ArcaneTrackerApplication.get().analytics.logEvent("menu_settings")

                SettingsCompanion.show()
            }

            view.findViewById<View>(R.id.games).setOnClickListener {
                ViewManager.get().removeView(view)

                ArcaneTrackerApplication.get().analytics.logEvent("menu_history")

                val intent = Intent()
                intent.setClass(ArcaneTrackerApplication.context, YourGamesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                ArcaneTrackerApplication.context.startActivity(intent)

            }

            view.findViewById<View>(R.id.yourDecks).setOnClickListener {
                mViewManager.removeView(view)

                ArcaneTrackerApplication.get().analytics.logEvent("menu_your_decks")


                val intent = Intent()
                intent.setClass(ArcaneTrackerApplication.context, YourDecksActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                ArcaneTrackerApplication.context.startActivity(intent)
            }

            view.findViewById<View>(R.id.yourPacks).setOnClickListener {
                mViewManager.removeView(view)

                ArcaneTrackerApplication.get().analytics.logEvent("menu_your_packs")


                val intent = Intent()
                intent.setClass(ArcaneTrackerApplication.context, YourPacksActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                ArcaneTrackerApplication.context.startActivity(intent)
            }

            view.findViewById<View>(R.id.quit).setOnClickListener { Utils.exitApp() }

            mViewManager.addMenu(view, v2)
        }

        handleView = v.findViewById(R.id.opponentHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.opponentColor))
        handleView.setOnClickListener(ClickListener(STATE_OPPONENT))

        handleView = v.findViewById(R.id.playerHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.colorPrimary))
        handleView.setOnClickListener(ClickListener(STATE_PLAYER))
    }


    companion object {
        private var sOpponentCompanion: DeckCompanion? = null
        private var sPlayerCompanion: DeckCompanion? = null

        val STATE_PLAYER = 0
        val STATE_OPPONENT = 1

        val opponentCompanion: DeckCompanion
            get() {
                get()
                return sOpponentCompanion!!
            }

        val playerCompanion: DeckCompanion
            get() {
                get()
                return sPlayerCompanion!!
            }

        private var sMainCompanion: MainViewCompanion? = null

        fun get(): MainViewCompanion {
            if (sMainCompanion == null) {
                val view = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.main_view, null)
                sMainCompanion = MainViewCompanion(view)
            }

            return sMainCompanion!!
        }
    }
}
