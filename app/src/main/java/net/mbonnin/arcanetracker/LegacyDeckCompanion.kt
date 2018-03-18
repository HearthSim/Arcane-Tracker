package net.mbonnin.arcanetracker

import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.hsmodel.Card

class LegacyDeckCompanion(v: View) : DeckCompanion(v) {
    override var deck: Deck?
        get() = super.deck
        set(value) {
            super.deck = value

            if (value == null) {
                return
            }

            PaperDb.write(KEY_LAST_USED_DECK_ID, value.id)
            winLoss.text = value.wins.toString() + " - " + value.losses

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
                    try {
                        value.wins = win.value
                    } catch (e: Exception) {
                        value.wins = 0
                    }

                    try {
                        value.losses = losses.value
                    } catch (e: Exception) {
                        value.losses = 0
                    }

                    LegacyDeckList.saveDeck(value)

                    MainViewCompanion.legacyCompanion.deck = deck
                }
                view2.findViewById<View>(R.id.cancel).setOnClickListener { v3 -> mViewManager.removeView(view2) }

                mViewManager.addCenteredView(view2)
            }
            Controller.get().setLegacyCardMap(value.cards)
        }

    init {
        EditButtonCompanion(settings)
        val lastUsedId = PaperDb.read<String>(KEY_LAST_USED_DECK_ID)

        var deck: Deck? = null
        if (lastUsedId != null) {
            for (deck2 in LegacyDeckList.get()) {
                if (deck2.id == lastUsedId) {
                    deck = deck2
                    break
                }
            }
            if (deck == null && lastUsedId == LegacyDeckList.ARENA_DECK_ID) {
                deck = LegacyDeckList.arenaDeck
            }
        }

        if (deck == null) {
            deck = LegacyDeckList.createDeck(Card.CLASS_INDEX_WARRIOR)
            PaperDb.write(KEY_LAST_USED_DECK_ID, deck!!.id)
        }

        recyclerView.adapter = Controller.get().legacyAdapter
        this.deck = deck
    }
    companion object {
        private val KEY_LAST_USED_DECK_ID = "KEY_LAST_USED_DECK_ID"
    }
}
