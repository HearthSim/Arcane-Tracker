package net.mbonnin.arcanetracker

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.NumberPicker
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.arcanetracker.room.WLCounter
import timber.log.Timber

class PlayerDeckCompanion(v: View) : DeckCompanion(v) {
    init {
        recyclerView.adapter = Controller.get().playerAdapter
        settings.setImageResource(R.drawable.ic_compare_arrows_black_24dp)
        settings.visibility = GONE

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.your_deck_will_appear))
    }

    var disposable: Disposable? = null
    var rdeck: RDeck? = null

    override var deck: Deck?
        get() = super.deck
        set(value) {
            super.deck = value

            if (value == null) {
                return
            }

            disposable?.dispose()

            Timber.d("setDeck")

            disposable = WLCounter.watch(value.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { rdeck ->
                        Timber.d("setText")
                        this.rdeck = rdeck
                        winLoss.text = rdeck.wins.toString() + " - " + rdeck.losses.toString()
                    }


            winLoss.setOnClickListener { v2 ->
                val view2 = LayoutInflater.from(v2.context).inflate(R.layout.edit_win_loss, null)

                val win = view2.findViewById<NumberPicker>(R.id.win)
                win.minValue = 0
                win.maxValue = 999
                rdeck?.wins?.let { win.value = it }
                val losses = view2.findViewById<NumberPicker>(R.id.loss)
                losses.minValue = 0
                losses.maxValue = 999
                rdeck?.losses?.let { losses.value = it }
                view2.findViewById<View>(R.id.ok).setOnClickListener { v3 ->
                    mViewManager.removeView(view2)

                    WLCounter.set(value.id, win.value, losses.value)
                }
                view2.findViewById<View>(R.id.cancel).setOnClickListener { v3 -> mViewManager.removeView(view2) }

                mViewManager.addCenteredView(view2)
            }

            Controller.get().setPlayerCardMap(value.cards)
        }
}