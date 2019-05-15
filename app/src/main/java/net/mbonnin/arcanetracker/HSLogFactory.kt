package net.mbonnin.arcanetracker

import android.os.Handler
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.hearthsim.console.Console
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

        hsLog.setListener(HSLogListener { hsLog.currentOrFinishedGame() })

        val handler = Handler()
        LogReader("LoadingScreen.log").start { rawLine, isOldData ->
            handler.post {
                hsLog.processLoadingScreen(rawLine, isOldData)
            }
        }

        LogReader("Power.log").start { rawLine, isOldData ->
            handler.post {
                hsLog.processPower(rawLine, isOldData)
            }
        }

        LogReader("Achievements.log").start { rawLine, isOldData ->
            handler.post {
                hsLog.processAchievement(rawLine, isOldData)
            }
        }

        LogReader("Decks.log").start { rawLine, isOldData ->
            handler.post {
                hsLog.processDecks(rawLine, isOldData)
            }
        }

        return hsLog
    }
}