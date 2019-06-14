package net.mbonnin.arcanetracker

import android.os.Handler
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mbonnin.arcanetracker.hslog.Console
import net.mbonnin.arcanetracker.hslog.HSLog
import net.mbonnin.arcanetracker.hslog.achievements.AchievementsParser
import net.mbonnin.arcanetracker.reader.LogReader
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.arcanetracker.room.RPack
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.hearthsim.hsmodel.CardJson
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

object HSLogFactory {
    fun createHSLog(console: Console, cardJson: CardJson): HSLog {
        val hsLog = HSLog(console, cardJson)
        hsLog.onPlayerDeckChanged {
            if (it.name.isNullOrBlank()) {
                it.name = Utils.getString(R.string.deck)
            }
            MainViewCompanion.playerCompanion.deck = it
        }
        hsLog.onOpponentDeckChanged {
            MainViewCompanion.opponentCompanion.deck = it
        }
        hsLog.onRawGame { gameStr, gameStart ->
            GameHelper.insertAndUploadGame(gameStr, Date(gameStart), hsLog)
        }
        hsLog.onGameEnd {
            GameHelper.gameEnded(it)
        }
        hsLog.onCardGained {
            cardGained(it)
        }
        hsLog.onNewDeckFound {deck, deckString, isArena ->
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

        val handler = Handler()
        /*
         * we need to read the whole loading screen if we start the Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        val loadingScreenLogReader = LogReader("LoadingScreen.log", false)
        loadingScreenLogReader.start(object : LogReader.LineConsumer {
            var previousDataRead = false
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processLoadingScreen(rawLine, previousDataRead)
                }
            }

            override fun onPreviousDataRead() {
                previousDataRead = true
            }
        })

        /*
         * Power.log, we just want the incremental changes
         */
        val powerLogReader = LogReader("Power.log", true)
        powerLogReader.start(object : LogReader.LineConsumer {
            var previousDataRead = false
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processPower(rawLine, previousDataRead)
                }
            }

            override fun onPreviousDataRead() {
                previousDataRead = true
            }
        })


        val achievementLogReader = LogReader("Achievements.log", true)
        achievementLogReader.start(object : LogReader.LineConsumer {
            var previousDataRead = false
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processAchievement(rawLine, previousDataRead)
                }
            }

            override fun onPreviousDataRead() {
                previousDataRead = true
            }
        })

        val decksLogReader = LogReader("Decks.log", false)
        decksLogReader.start(object : LogReader.LineConsumer {
            var previousDataRead = false
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processDecks(rawLine, previousDataRead)
                }
            }

            override fun onPreviousDataRead() {
                previousDataRead = true
            }
        })

        return hsLog
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