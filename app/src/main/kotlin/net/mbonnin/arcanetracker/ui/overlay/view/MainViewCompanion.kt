package net.mbonnin.arcanetracker.ui.overlay.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.hearthsim.hslog.DeckEntry
import net.hearthsim.hslog.PossibleSecret
import net.hearthsim.hsmodel.battlegrounds.battlegroundsMinions
import net.hearthsim.hsmodel.enum.Race
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.ui.my_games.YourGamesActivity
import net.mbonnin.arcanetracker.ui.my_packs.YourPacksActivity
import net.mbonnin.arcanetracker.ui.overlay.Onboarding
import net.mbonnin.arcanetracker.ui.overlay.Onboarding.hsReplayHandleClicked
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller
import net.mbonnin.arcanetracker.ui.overlay.adapter.ItemAdapter
import net.mbonnin.arcanetracker.ui.settings.SettingsCompanion
import net.mbonnin.arcanetracker.ui.stats.YourDecksActivity

class MainViewCompanion(val mainView: View) {
    private val mHandler: Handler
    private val frameLayout: View

    private val playerView: View
    private val opponentView: View
    private val secretView: View
    private val battlegroundsView: View
    private val mViewManager = ViewManager.get()

    val handlesView = LayoutInflater.from(mainView.context).inflate(R.layout.handles_view, null) as HandlesView

    private var state: Int = 0

    private var mWidth = 0
    val secretsAdapter = ItemAdapter()
    val battlegroundsAdapter = ItemAdapter()

    private val drawerHelper = DrawerHelper(mainView, handlesView, DrawerHelper.Edge.LEFT)
    fun setAlpha(progress: Int) {
        drawerHelper.setAlpha(progress)
    }

    val battlegroundDeckEntries by lazy {
        battlegroundsMinions.map {
            DeckEntry.Item(card = ArcaneTrackerApplication.get().cardJson.getCard(it.cardId),
                    count = 1,
                    entityList = emptyList(),
                    gift = false,
                    techLevel = it.techLevel)
        }.sortedByDescending {
            10 * it.techLevel!! + when(it.card.race) {
                Race.ALL -> 9
                Race.MURLOC -> 8
                Race.BEAST -> 7
                Race.DEMON -> 6
                Race.MECHANICAL -> 5
                else -> 4
            }
        }
    }

    init {
        mHandler = Handler()

        frameLayout = mainView.findViewById(R.id.frameLayout)
        opponentView = mainView.findViewById(R.id.opponentView)
        secretView = mainView.findViewById(R.id.secretRecyclerView)
        playerView = mainView.findViewById(R.id.playerView)
        battlegroundsView = mainView.findViewById(R.id.battlegroundsRecyclerView)

        mWidth = Settings.get(Settings.DRAWER_WIDTH, 0)
        if (mWidth < minDrawerWidth || mWidth >= maxDrawerWidth) {
            mWidth = (0.33 * 0.5 * mViewManager.width.toDouble()).toInt()
        }
        setDrawerWidth(mWidth)

        sOpponentCompanion = OpponentDeckCompanion(opponentView)
        sPlayerCompanion = PlayerDeckCompanion(playerView)
        val recyclerView = mainView.findViewById<RecyclerView>(R.id.secretRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(mainView.context)
        recyclerView.adapter = secretsAdapter

        val battlegroundsRecyclerView = mainView.findViewById<RecyclerView>(R.id.battlegroundsRecyclerView)
        battlegroundsRecyclerView.layoutManager = LinearLayoutManager(mainView.context)
        battlegroundsRecyclerView.adapter = battlegroundsAdapter

        drawerHelper.setViewHeight(mViewManager.height)

        configureHandles(handlesView)

        setState(STATE_PLAYER, false)
    }

    fun onBattlegrounds(list: List<DeckEntry>) {
        handlesView.findViewById<View>(R.id.battlegroundsHandle).visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        handlesView.findViewById<View>(R.id.playerHandle).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        handlesView.findViewById<View>(R.id.opponentHandle).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        if (!list.isEmpty()) {
            val list2 = mutableListOf<DeckEntry>()
            list2.add(DeckEntry.Text(handlesView.context.getString(R.string.battlegroundsOpponents)))
            list2.addAll(list)
            list2.add(DeckEntry.Text(handlesView.context.getString(R.string.minions)))
            list2.addAll(battlegroundDeckEntries)
            battlegroundsAdapter.setList(list2)
        }
    }

    fun onSecrets(possibleSecrets: List<PossibleSecret>) {
        val list = mutableListOf<DeckEntry>()

        list.add(DeckEntry.Secrets)

        list.addAll(possibleSecrets.map { secret ->
            val card = CardUtil.getCard(secret.cardId)
            DeckEntry.Item(
                    card = card,
                    count = secret.count,
                    entityList = emptyList()
            )
        })

        secretsAdapter.setList(list)

        handlesView.findViewById<View>(R.id.secretHandle).visibility = if (possibleSecrets.isEmpty()) View.GONE else View.VISIBLE

        drawerHelper.notifyHandlesChanged()
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
        drawerHelper.notifyHandlesChanged(resetY = true)
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
            secretView.visibility = View.GONE
            battlegroundsView.visibility = View.GONE

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
                STATE_SECRET -> {
                    secretView.visibility = View.VISIBLE
                    ArcaneTrackerApplication.get().analytics.logEvent("state_secret")
                }
                STATE_BATTLEGROUNDS -> {
                    battlegroundsView.visibility = View.VISIBLE
                    ArcaneTrackerApplication.get().analytics.logEvent("state_battlegrounds")
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

    private fun configureHandles(v: HandlesView) {
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

        handleView = v.findViewById(R.id.secretHandle)
        drawable = v.context.resources.getDrawable(R.drawable.ic_question)
        handleView.init(drawable, Color.BLACK)
        handleView.setOnClickListener(ClickListener(STATE_SECRET))

        handleView = v.findViewById(R.id.battlegroundsHandle)
        drawable = v.context.resources.getDrawable(R.drawable.swords)
        handleView.init(drawable, v.context.resources.getColor(R.color.battlegroundsColor))
        handleView.setOnClickListener(ClickListener(STATE_BATTLEGROUNDS))

        //handleView.visibility = View.VISIBLE
        //battlegroundsAdapter.setList(battlegroundDeckEntries)

        handleView = v.findViewById(R.id.opponentHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.opponentColor))
        handleView.setOnClickListener(ClickListener(STATE_OPPONENT))

        handleView = v.findViewById(R.id.playerHandle)
        drawable = v.context.resources.getDrawable(R.drawable.icon_white)
        handleView.init(drawable, v.context.resources.getColor(R.color.colorPrimary))
        handleView.setOnClickListener(ClickListener(STATE_PLAYER))

        v.orientation = LinearLayout.VERTICAL

        drawerHelper.notifyHandlesChanged(resetY = true)
    }

    companion object {
        private var sOpponentCompanion: DeckCompanion? = null
        private var sPlayerCompanion: DeckCompanion? = null

        val STATE_PLAYER = 0
        val STATE_OPPONENT = 1
        val STATE_SECRET = 2
        val STATE_BATTLEGROUNDS = 3

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
