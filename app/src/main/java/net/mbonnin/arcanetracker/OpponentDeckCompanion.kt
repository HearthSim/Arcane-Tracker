package net.mbonnin.arcanetracker

import android.view.View
import net.mbonnin.arcanetracker.adapter.Controller

class OpponentDeckCompanion(v: View): DeckCompanion(v) {
    init {
        settings.visibility = View.GONE
        winLoss.visibility = View.GONE

        recyclerView.adapter = Controller.get().opponentAdapter
        deck = DeckList.getOpponentDeck()
    }
}