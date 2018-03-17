package net.mbonnin.arcanetracker

import android.view.View
import net.mbonnin.arcanetracker.adapter.Controller

class PlayerDeckCompanion(v: View): DeckCompanion(v) {
    init {
        recyclerView.adapter = Controller.get().playerAdapter
        settings.visibility = View.GONE

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