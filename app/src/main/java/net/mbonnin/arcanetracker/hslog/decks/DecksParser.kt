package net.mbonnin.arcanetracker.hslog.decks

import net.hearthsim.kotlin.hslog.LogLine
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.hslog.Deck
import net.mbonnin.hsmodel.CardJson
import timber.log.Timber

class DecksParser(private val cardJson: CardJson, val onPlayerDeckChanged: (Deck) -> Unit, val onNewDeckFound: (Deck, String, Boolean) -> Unit) {
    enum class State {
        DEFAULT,
        ARENA,
        GAME
    }

    private val deckStringHelper = DeckStringHelper()
    var state = State.DEFAULT


    fun process(rawLine: String, isOldData: Boolean) {
        Timber.d(rawLine)
        if (rawLine.contains("Deck Contents Received:")) {
            state = State.DEFAULT
        } else if (rawLine.contains("Finished Editing Deck:")) {
            state = State.DEFAULT
        } else if (rawLine.contains("Finding Game With Deck:")) {
            state = State.GAME
        } else if (rawLine.contains("Starting Arena Game With Deck")) {
            state = State.ARENA
        } else {
            val logLine = LogLine.parseLine(rawLine)
            if (logLine != null) {
                val result = deckStringHelper.parseLine(logLine.line)

                if (result?.id != null) {
                    val deck = DeckStringHelper.parse(result.deckString, cardJson)
                    if (deck != null) {
                        deck.id = result.id
                        if (state == State.ARENA) {
                            deck.name = ArcaneTrackerApplication.get().getString(R.string.arenaDeck)
                        } else {
                            deck.name = result.name ?: "?"
                        }

                        if (state == State.ARENA || state == State.GAME) {
                            onPlayerDeckChanged(deck)
                        }
                        onNewDeckFound(deck, result.deckString, state == DecksParser.State.ARENA)
                    }
                }

            }
        }
    }
}
