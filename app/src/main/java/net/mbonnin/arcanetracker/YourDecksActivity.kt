package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.adapter.ItemAdapter

class YourDecksActivity : Activity() {
    lateinit var deckRecyclerView: RecyclerView
    lateinit var opponentRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.your_decks_activity)

        opponentRecyclerView = findViewById(R.id.opponentList)
        deckRecyclerView = findViewById(R.id.deckList)

        deckRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        opponentRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        setDeckId(null)
    }

    private fun setDeckId(deckId: String?) {
        findViewById<View>(R.id.opponents).visibility = if (deckId == null) GONE else VISIBLE
        opponentRecyclerView.visibility = if (deckId == null) GONE else VISIBLE

        findViewById<View>(R.id.selectDeck).visibility = if (deckId == null) VISIBLE else GONE

        if (deckId == null) {
            deckRecyclerView.setAdapter(LogsDeckAdapter())
        } else {
            val itemAdapter = ItemAdapter()
            val deck = LogsDeckList.get().filter { it.id == deckId }.firstOrNull()

            if (deck == null) {
                return
            }

            itemAdapter.setList(Controller.getCardMapList(deck.cards))
            deckRecyclerView.setAdapter(itemAdapter)

            opponentRecyclerView.adapter = OpponentsAdapter()
        }
    }
}

