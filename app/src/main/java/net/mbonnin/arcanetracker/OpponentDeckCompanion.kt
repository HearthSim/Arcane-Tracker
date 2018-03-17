package net.mbonnin.arcanetracker

import android.view.View
import android.widget.TextView
import net.mbonnin.arcanetracker.adapter.Controller

class OpponentDeckCompanion(v: View): DeckCompanion(v) {
    init {
        settings.visibility = View.GONE
        winLoss.visibility = View.GONE

        recyclerView.adapter = Controller.get().opponentAdapter

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.opponent_deck_will_appear))
    }
}