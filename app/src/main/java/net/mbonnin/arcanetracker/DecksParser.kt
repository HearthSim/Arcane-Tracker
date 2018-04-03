package net.mbonnin.arcanetracker

import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import net.mbonnin.arcanetracker.parser.LogReader
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DecksParser: LogReader.LineConsumer {
    var isArena = false
    val deckStringHelper = DeckStringHelper()
    val subject = PublishSubject.create<Unit>()

    init {
        subject.debounce(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Timber.d("cleanup DB")
                    RDatabaseSingleton.instance.deckDao().cleanup()
                }, Timber::e)
    }

    override fun onLine(rawLine: String) {
        if (rawLine.contains("Deck Contents Received:")) {
            isArena = false
        } else if (rawLine.contains("Finished Editing Deck:")) {
            isArena = false
        } else if (rawLine.contains("Finding Game With Deck:")) {
            isArena = false
        } else if (rawLine.contains("Starting Arena Game With Deck")) {
            isArena = true
        } else {
            val logLine = LogReader.parseLine(rawLine)
            if (logLine != null) {

                Timber.d(logLine.line)

                val result = deckStringHelper.parseLine(logLine.line)

                if (result != null && result.id != null) {
                    val deck = DeckStringParser.parse(result.deckString)
                    if (deck != null) {
                        deck.id = result.id
                        if (isArena) {
                            deck.name = ArcaneTrackerApplication.get().getString(R.string.arenaDeck)
                        } else {
                            deck.name = result.name
                        }

                        Completable.fromAction {
                            MainViewCompanion.playerCompanion.deck = deck
                        }
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe()

                        val rdeck = RDeck(id = deck.id,
                                name = deck.name,
                                deck_string = result.deckString,
                                arena = isArena)

                        try {
                            RDatabaseSingleton.instance.deckDao().insert(rdeck)
                        } catch (e: Exception) {
                            RDatabaseSingleton.instance.deckDao().updateNameAndContents(rdeck.id, rdeck.name, rdeck.deck_string, rdeck.accessMillis)
                        }

                        subject.onNext(Unit)
                    }
                }

            }
        }
    }

    override fun onPreviousDataRead() {
    }

    companion object {
        var instance: DecksParser? = null
        fun get(): DecksParser {
            if (instance == null) {
                instance = DecksParser()
            }

            return instance!!
        }
    }
}
