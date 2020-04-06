package net.mbonnin.arcanetracker

import android.os.Handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hearthsim.console.Console
import net.hearthsim.hslog.HSLog
import net.hearthsim.hsmodel.CardJson
import net.mbonnin.arcanetracker.reader.LogReader
import java.io.File

object HSLogFactory {
    fun createHSLog(console: Console, cardJson: CardJson): HSLog {
        val hsLog = HSLog(console, cardJson)

        hsLog.setListener(ATHSLogListener {hsLog.currentOrFinishedGame()})

        val handler = Handler()
        /*
         * we need to read the whole loading screen if we start the Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        val loadingScreenLogReader = LogReader("LoadingScreen.log", false)
        loadingScreenLogReader.start(object : LogReader.LineConsumer {
            var isOldData = true
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processLoadingScreen(rawLine, isOldData)
                }
            }

            override fun onPreviousDataRead() {
                isOldData = false
            }
        })


        if (false) {
            configureDebugReader(hsLog, handler)
        } else {
            /*
             * Power.log, we just want the incremental changes
             */
            val powerLogReader = LogReader("Power.log", true)
            powerLogReader.start(object : LogReader.LineConsumer {
                var isOldData = true
                override fun onLine(rawLine: String) {
                    handler.post {
                        hsLog.processPower(rawLine, isOldData)
                    }
                }

                override fun onPreviousDataRead() {
                    isOldData = false
                }
            })
        }


        val achievementLogReader = LogReader("Achievements.log", true)
        achievementLogReader.start(object : LogReader.LineConsumer {
            var isOldData = true
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processAchievement(rawLine, isOldData)
                }
            }

            override fun onPreviousDataRead() {
                isOldData = false
            }
        })

        val decksLogReader = LogReader("Decks.log", false)
        decksLogReader.start(object : LogReader.LineConsumer {
            var isOldData = true
            override fun onLine(rawLine: String) {
                handler.post {
                    hsLog.processDecks(rawLine, isOldData)
                }
            }

            override fun onPreviousDataRead() {
                isOldData = false
            }
        })

        return hsLog
    }

    private fun configureDebugReader(hsLog: HSLog, handler: Handler) {
        val reader = File("/sdcard/2019_11_16_19-51_battlegrounds").bufferedReader()

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val line = reader.readLine()
                if (line == null) {
                    break
                }
                if (line.contains("tag=NEXT_STEP value=MAIN_READY")) {
                    delay(2000)
                }
                handler.post {
                    hsLog.processPower(line, false)
                }
            }
        }
    }
}