package net.mbonnin.arcanetracker

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.*
import net.hearthsim.hslog.parser.achievements.AchievementsParser
import net.hearthsim.hslog.parser.power.Game
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.arcanetracker.room.RPack
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ATHSLogListener(val currentOrFinishedGame: () -> Game?): HSLogListener {

    override fun onGameStart(game: Game) {
    }

    override fun onGameChanged(game: Game) {
    }

    override fun onGameEnd(game: Game) {
        GameHelper.gameEnded(game)
        TurnTimer.gameEnd(game)
    }

    override fun onSecrets(possibleSecrets: List<PossibleSecret>) {

    }

    override fun onRawGame(gameString: String, gameStartMillis: Long) {
        val url = BuildConfig.DEBUG_URL
        if (!url.isBlank()) {
            GlobalScope.launch {
                try {
                    HttpClient().post<Unit>(url) {
                        body = gameString
                    }
                } catch(e: Exception) {
                    Timber.e("could not upload debug data: ${e.message}")
                }
            }
        }
        GameHelper.insertAndUploadGame(gameString, Date(gameStartMillis), currentOrFinishedGame)
    }

    override fun onCardGained(cardGained: AchievementsParser.CardGained) {
        cardGained(cardGained)
    }

    override fun onDeckFound(deck: Deck, deckString: String, isArena: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            val rdeck = RDeck(id = deck.id!!,
                    name = deck.name!!,
                    deck_string = deckString,
                    arena = isArena)

            try {
                RDatabaseSingleton.instance.deckDao().insert(rdeck)
            } catch (e: Exception) {
                RDatabaseSingleton.instance.deckDao().updateNameAndContents(rdeck.id, rdeck.name, rdeck.deck_string, rdeck.accessMillis)
            }
        }
    }

    private fun getDeckEntriesFromCardMap(cardMap: Map<String, Int>): List<DeckEntry> {
        val deckEntryList = mutableListOf<DeckEntry.Item>()
        val list = mutableListOf<DeckEntry>()
        var unknown = Deck.MAX_CARDS

        for ((key, value) in cardMap) {
            val deckEntry = DeckEntry.Item(
                    card = CardUtil.getCard(key),
                    count = value,
                    entityList = emptyList())

            deckEntryList.add(deckEntry)
            unknown -= deckEntry.count
        }

        list.addAll(deckEntryList.sortedWith(ControllerCommon.deckEntryComparator))

        if (unknown > 0) {
            list.add(DeckEntry.Unknown(unknown))
        }
        return list
    }

    override fun onPlayerDeckChanged(deck: Deck) {
        if (deck.name.isNullOrBlank()) {
            deck.name = Utils.getString(R.string.deck)
        }
        MainViewCompanion.playerCompanion.deck = deck
        Controller.get().onDeckEntries(null, true, getDeckEntriesFromCardMap(deck.cards))
    }

    override fun onOpponentDeckChanged(deck: Deck) {
        MainViewCompanion.opponentCompanion.deck = deck
    }

    override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
        TurnTimer.onTurn(game, turn, isPlayer)
    }
    private val cardList = mutableListOf<AchievementsParser.CardGained>()
    private var disposable: Disposable? = null

    fun cardGained(cardGained: AchievementsParser.CardGained) {
        synchronized(this) {
            cardList.add(AchievementsParser.CardGained(cardGained.id, cardGained.golden))
        }

        // if some delay pass without a new card incoming, we consider the pack done
        disposable?.dispose()
        disposable = Completable.complete().delay(2000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe {
                    synchronized(this) {
                        if (cardList.size == 5) {
                            val dust = cardList.sumBy {
                                val card = CardUtil.getCard(it.id)
                                CardUtil.getDust(card.rarity, it.golden)
                            }
                            val rPack = RPack(cardList = cardList.map { it.toString() }.joinToString(","), dust = dust)
                            RDatabaseSingleton.instance.packDao().insert(rPack)
                        } else {
                            Timber.e("wrong number of cards in pack: ${cardList.size}")
                        }
                        cardList.clear()
                    }
                }
    }

    override fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>) {
        Controller.get().onDeckEntries(game, isPlayer, deckEntries)
    }

}