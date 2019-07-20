package net.mbonnin.arcanetracker

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.hearthsim.hslog.Deck
import net.hearthsim.hslog.HSLog
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

class HSLogListener(val currentOrFinishedGame: () -> Game?): HSLog.Listener {

    override fun onGameStart(game: Game) {
        Controller.get().gameStarted(game)
    }

    override fun onGameChanged(game: Game) {
        Controller.get().somethingChanged()
    }

    override fun onGameEnd(game: Game) {
        GameHelper.gameEnded(game)
        TurnTimer.gameEnd(game)
    }

    override fun onRawGame(gameString: String, gameStartMillis: Long) {
        val url = BuildConfig.DEBUG_URL
        if (!url.isBlank()) {
            GlobalScope.launch {
                HttpClient().post<Unit>(url) {
                    body = gameString
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

    override fun onPlayerDeckChanged(deck: Deck) {
        if (deck.name.isNullOrBlank()) {
            deck.name = Utils.getString(R.string.deck)
        }
        MainViewCompanion.playerCompanion.deck = deck
    }

    override fun onOpponentDeckChanged(deck: Deck) {
        MainViewCompanion.opponentCompanion.deck = deck
    }

    override fun onTurn(game: Game, turn: Int, player: Boolean) {
        TurnTimer.onTurn(game, turn, player)
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

}