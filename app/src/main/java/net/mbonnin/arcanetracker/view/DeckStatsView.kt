package net.mbonnin.arcanetracker.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import net.mbonnin.arcanetracker.Deck
import net.mbonnin.arcanetracker.OpponentsAdapter
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.adapter.ItemAdapter

class DeckStatsView(context: Context, val deck: Deck): ConstraintLayout(context) {
    val deckRecyclerView: RecyclerView
    val opponentRecyclerView: RecyclerView
    val opponents: View
    val deckName: TextView
    val deckBackground: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.your_decks_view, this, true)

        opponentRecyclerView = findViewById(R.id.opponentList)
        deckRecyclerView = findViewById(R.id.deckList)
        opponents = findViewById(R.id.opponents)
        deckBackground = findViewById(R.id.deckBackground)
        deckName = findViewById(R.id.deckName)

        deckRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        opponentRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val itemAdapter = ItemAdapter()

        deckName.setText(deck.name)
        deckBackground.setImageDrawable(Utils.getDrawableForClassIndex(deck.classIndex))

        itemAdapter.setList(Controller.getCardMapList(deck.cards))
        deckRecyclerView.setAdapter(itemAdapter)

        opponentRecyclerView.adapter = OpponentsAdapter(deck.id)

    }
}