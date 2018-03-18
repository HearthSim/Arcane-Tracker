package net.mbonnin.arcanetracker

import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck

class PlayerDeckCompanion(v: View) : DeckCompanion(v) {
    init {
        recyclerView.adapter = Controller.get().playerAdapter
        settings.visibility = View.GONE

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.your_deck_will_appear))
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

            disposable = RDatabaseSingleton.instance.deckDao().findById(value.id)
                    .onErrorReturn {
                        val rdeck = RDeck()
                        rdeck.id = value.id
                        rdeck
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { rdeck ->
                        winLoss.text = rdeck.wins.toString() + " - " + rdeck.losses.toString()
                    }


            winLoss.setOnClickListener { v2 ->
                val view2 = LayoutInflater.from(v2.context).inflate(R.layout.edit_win_loss, null)

                val win = view2.findViewById<NumberPicker>(R.id.win)
                win.minValue = 0
                win.maxValue = 999
                win.value = value.wins
                val losses = view2.findViewById<NumberPicker>(R.id.loss)
                losses.minValue = 0
                losses.maxValue = 999
                losses.value = value.losses
                view2.findViewById<View>(R.id.ok).setOnClickListener { v3 ->
                    mViewManager.removeView(view2)

                    val rdeck2 = RDeck()
                    rdeck2.id = value.id
                    rdeck2.wins = win.value
                    rdeck2.losses = losses.value

                    Observable.fromCallable {
                        RDatabaseSingleton.instance.deckDao().insert(rdeck2)
                    }.subscribeOn(Schedulers.io())
                            .subscribe()
                }
                view2.findViewById<View>(R.id.cancel).setOnClickListener { v3 -> mViewManager.removeView(view2) }

                mViewManager.addCenteredView(view2)
            }

            Controller.get().setPlayerCardMap(value.cards)
        }
}