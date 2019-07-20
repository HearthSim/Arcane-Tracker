package net.mbonnin.arcanetracker.ui.overlay.view

import android.view.View
import android.widget.TextView
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller

class OpponentDeckCompanion(v: View): DeckCompanion(v) {
    init {
        settings.visibility = View.GONE
        winLoss.visibility = View.GONE

        recyclerView.adapter = Controller.get().opponentAdapter

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.opponent_deck_will_appear))
    }
}