package net.mbonnin.arcanetracker

import android.view.LayoutInflater
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hearthsim.hslog.ControllerCommon
import net.hearthsim.hslog.DeckEntry
import net.hearthsim.hslog.HSLogListener
import net.hearthsim.hslog.parser.power.PossibleSecret
import net.hearthsim.hslog.parser.achievements.CardGained
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.GameType
import net.hearthsim.hsreplay.HSReplayResult
import net.hearthsim.hsreplay.execute
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.arcanetracker.room.RPack
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.jolly.JollyRequest
import net.mbonnin.jolly.Method
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ATHSLogListener(private val currentOrFinishedGame: () -> Game?): HSLogListener {

    private var toastContainer: ToastContainer? = null

    override fun onGameStart(game: Game) {
    }

    override fun bgHeroesShow(game: Game, entities: List<Entity>) {
        GlobalScope.launch(Dispatchers.Main) {
            // leave some time for the Heroes to be displayed
            delay(1000)
            val toast = LayoutInflater.from(ArcaneTrackerApplication.context)
                .inflate(R.layout.toast_compare_heroes, null, false)

            toast.setOnClickListener {
                val ids = entities.map { it.card?.dbfId }.joinToString(",")
                Utils.openLink("https://hsreplay.net/battlegrounds/heroes?utm_source=arcanetracker&utm_medium=client" +
                    "&utm_campaign=bgs_toast#heroes=$ids")
                bgHeroesHide()
            }
            toastContainer = Toaster.show(toast, 30000L)
        }
    }

    override fun bgHeroesHide() {
        toastContainer?.let {
            Toaster.dismiss(it)
            toastContainer = null
        }
    }

    override fun onGameChanged(game: Game) {
    }

    override fun onGameEnd(game: Game) {
        GameHelper.gameEnded(game)
        TurnTimer.gameEnd(game)
    }

    override fun onSecrets(possibleSecrets: List<PossibleSecret>) {
        MainViewCompanion.get().onSecrets(possibleSecrets)
    }

    override fun onRawGame(gameString: ByteArray, gameStartMillis: Long) {
        val url = BuildConfig.DEBUG_URL
        if (!url.isBlank()) {
            GlobalScope.launch {
                JollyRequest {
                    method(Method.POST)
                    body("text/play", gameString)
                    url(url)
                }.execute().let {
                    if (it is HSReplayResult.Error) {
                        Timber.e("uploadbin error: ${it.exception.message}")
                    }
                }
            }
        }
        GameHelper.insertAndUploadGame(gameString, Date(gameStartMillis), currentOrFinishedGame)
    }

    override fun onCardGained(cardGained: CardGained) {
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
        Controller.get().onDeckEntries(true, getDeckEntriesFromCardMap(deck.cards))
    }

    override fun onOpponentDeckChanged(deck: Deck) {
        MainViewCompanion.opponentCompanion.deck = deck
    }

    override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
        TurnTimer.onTurn(game, turn, isPlayer)
    }
    private val cardList = mutableListOf<CardGained>()
    private var disposable: Disposable? = null

    fun cardGained(cardGained: CardGained) {
        synchronized(this) {
            cardList.add(CardGained(cardGained.id, cardGained.golden))
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
        if (deckEntries.firstOrNull { it is DeckEntry.Hero } != null) {
            // this is a battlegrounds board
            MainViewCompanion.get().onBattlegrounds(game.gameEntity?.tags?.get(Entity.KEY_TURN)?.toIntOrNull(), deckEntries)
        } else {
            Controller.get().onDeckEntries(isPlayer, deckEntries)
            MainViewCompanion.get().onBattlegrounds(null, emptyList())
        }
    }
}