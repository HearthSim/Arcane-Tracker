package net.mbonnin.arcanetracker.ui.overlay.view

import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.deck_view.*
import net.hearthsim.hslog.parser.decks.Deck
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.room.WLCounter
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller
import net.mbonnin.arcanetracker.ui.overlay.adapter.PlayerDeckListAdapter
import timber.log.Timber


class PlayerDeckCompanion(override val containerView: View) : DeckCompanion(containerView), LayoutContainer {
    init {
        recyclerView.adapter = Controller.get().playerAdapter
        settings.setImageResource(R.drawable.ic_compare_arrows_white_24dp)

        val viewManager = ViewManager.get()


        settings.setOnClickListener { v2 ->
            ArcaneTrackerApplication.get().analytics.logEvent("edit_swap")

            val a = IntArray(2)
            settings.getLocationOnScreen(a)

            val deckListView = LayoutInflater.from(v2.getContext()).inflate(R.layout.decklist_view, null)
            val recyclerView = deckListView.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(v2.getContext())
            recyclerView.setBackgroundColor(ArcaneTrackerApplication.context.resources.getColor(R.color.dialogGray))
            val adapter = PlayerDeckListAdapter.get()
            adapter.setOnDeckSelectedListener { deck ->
                viewManager.removeView(deckListView)
                ArcaneTrackerApplication.get().hsLog.setDeck(deck)
            }
            recyclerView.adapter = adapter

            val params = ViewManager.Params()
            params.x = a[0] + settings.width / 2 + Utils.dpToPx(20)
            params.y = 0
            params.w = Utils.dpToPx(250)
            params.h = a[1] + settings.height / 2

            viewManager.addModalView(deckListView, params)
        }

        val resId = if (!HearthstoneFilesDir.logExists("Achievements.log") && !Settings.get(Settings.HAS_SEEN_POWER_LOG, false)) {
            R.string.please_restart_hearthstone
        } else {
            R.string.your_deck_will_appear
        }
        text.setText(containerView.context.getString(resId))
    }

    var disposable: Disposable? = null

    override var deck: Deck?
        get() = super.deck
        set(value) {
            super.deck = value

            if (value == null) {
                return
            }

            disposable?.dispose()

            Timber.d("setDeck ${value.name}")

            disposable = WLCounter.watch(value.id!!)
                    .startWith(WLCounter(0, 0))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { counter ->
                        Timber.d("setText")
                        winLoss.text = counter.wins.toString() + " - " + counter.losses.toString()
                    }


            winLoss.setOnClickListener { v2 ->
                val view2 = LayoutInflater.from(v2.context).inflate(R.layout.edit_win_loss, null)

                val win = view2.findViewById<NumberPicker>(R.id.win)
                win.minValue = 0
                win.maxValue = 999
                val losses = view2.findViewById<NumberPicker>(R.id.loss)
                losses.minValue = 0
                losses.maxValue = 999

                view2.findViewById<View>(R.id.ok).setOnClickListener {
                    mViewManager.removeView(view2)

                    WLCounter.set(value.id!!, win.value, losses.value)
                }
                view2.findViewById<View>(R.id.cancel).setOnClickListener { mViewManager.removeView(view2) }

                mViewManager.addCenteredView(view2)
            }
        }
}