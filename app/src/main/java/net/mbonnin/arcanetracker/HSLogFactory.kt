package net.mbonnin.arcanetracker

import android.os.Handler
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.hearthsim.hslog.Console
import net.hearthsim.hslog.HSLog
import net.hearthsim.hslog.parser.achievements.AchievementsParser
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

        hsLog.setListener(HSLogListener {hsLog.currentOrFinishedGame()})

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
}