package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Bundle

class YourDecksActivity : Activity() {
    var isInStats = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(YourDecksView(this, this::setDeck))
    }

    private fun setDeck(deck: Deck) {
        isInStats = true
        setContentView(DeckStatsView(this, deck))
    }

    override fun onBackPressed() {
        if (isInStats) {
            isInStats = false
            setContentView(YourDecksView(this, this::setDeck))
        } else {
            finish()
        }
    }
}

