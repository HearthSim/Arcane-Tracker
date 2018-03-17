package net.mbonnin.arcanetracker

import android.view.View
import android.widget.TextView
import net.mbonnin.arcanetracker.adapter.Controller

class PlayerDeckCompanion(v: View): DeckCompanion(v) {
    init {
        recyclerView.adapter = Controller.get().playerAdapter
        settings.visibility = View.GONE

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.your_deck_will_appear))
    }

    override var deck: Deck?
        get() = super.deck
        set(value) {
            super.deck = value

            if (value == null) {
                return
            }

            Controller.get().setPlayerCardMap(value.cards)
        }
}