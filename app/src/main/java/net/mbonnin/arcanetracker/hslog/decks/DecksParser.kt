package net.mbonnin.arcanetracker.hslog.decks

import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import net.hearthsim.kotlin.hslog.LogLine
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.helper.DeckStringHelper
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import timber.log.Timber

class DecksParser {
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
                    val deck = DeckStringHelper.parse(result.deckString)
                    if (deck != null) {
                        deck.id = result.id
                        if (state == State.ARENA) {
                            deck.name = ArcaneTrackerApplication.get().getString(R.string.arenaDeck)
                        } else {
                            deck.name = result.name ?: "?"
                        }

                        if (state == State.ARENA || state == State.GAME) {
                            Completable.fromAction {
                                MainViewCompanion.playerCompanion.deck = deck
                            }
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .subscribe()
                        }

                        val rdeck = RDeck(id = deck.id!!,
                                name = deck.name!!,
                                deck_string = result.deckString,
                                arena = state == State.ARENA)

                        try {
                            RDatabaseSingleton.instance.deckDao().insert(rdeck)
                        } catch (e: Exception) {
                            RDatabaseSingleton.instance.deckDao().updateNameAndContents(rdeck.id, rdeck.name, rdeck.deck_string, rdeck.accessMillis)
                        }
                    }
                }

            }
        }
    }
}
