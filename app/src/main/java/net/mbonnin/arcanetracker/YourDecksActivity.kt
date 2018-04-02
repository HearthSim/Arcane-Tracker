package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.adapter.ItemAdapter

class YourDecksActivity : Activity() {
    lateinit var deckRecyclerView: RecyclerView
    lateinit var opponentRecyclerView: RecyclerView
    lateinit var opponents: View
    lateinit var selectDeck: View
    lateinit var deckName: TextView
    lateinit var deckBackground: ImageView
    lateinit var button: Button

    var _deckId: String? = null
    val yourDecksAdapter = YourDecksAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.your_decks_activity)

        opponentRecyclerView = findViewById(R.id.opponentList)
        deckRecyclerView = findViewById(R.id.deckList)
        opponents = findViewById(R.id.opponents)
        selectDeck = findViewById(R.id.selectDeck)
        deckBackground = findViewById(R.id.deckBackground)
        deckName = findViewById(R.id.deckName)
        button = findViewById(R.id.button)

        deckRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        opponentRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        yourDecksAdapter.setOnClickListener{setDeckId(it)}

        button.setOnClickListener {
            if (_deckId != null) {
                setDeckId(null)
            } else {
                finish()
            }
        }
        setDeckId(null)
    }

    private fun setDeckId(deckId: String?) {
        _deckId = deckId
        
        opponents.visibility = if (deckId == null) GONE else VISIBLE
        opponentRecyclerView.visibility = if (deckId == null) GONE else VISIBLE
        selectDeck.visibility = if (deckId == null) VISIBLE else GONE
        deckBackground.visibility = if (deckId != null) VISIBLE else GONE
        deckName.visibility = if (deckId != null) VISIBLE else GONE

        if (deckId == null) {
            deckRecyclerView.setAdapter(yourDecksAdapter)
        } else {
            val itemAdapter = ItemAdapter()
            val deck = yourDecksAdapter.list.filter { it.id == deckId }.firstOrNull()

            if (deck == null) {
                return
            }

            deckName.setText(deck.name)
            deckBackground.setImageDrawable(Utils.getDrawableForClassIndex(deck.classIndex))


            itemAdapter.setList(Controller.getCardMapList(deck.cards))
            deckRecyclerView.setAdapter(itemAdapter)

            opponentRecyclerView.adapter = OpponentsAdapter()
        }
    }
}

