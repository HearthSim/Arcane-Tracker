package net.mbonnin.arcanetracker.parser

import timber.log.Timber
import java.util.regex.Pattern

/**
 * Created by martin on 11/7/16.
 */

class LoadingScreenParser private constructor() : LogReader.LineConsumer {

    private var mReadingPreviousData = true

    private var mParsedMode: String? = null

    @Volatile
    var mode: String? = MODE_UNKNOWN
        private set

    // the last mode before entering gameplay
    /*
     * this is called from multiple threads
     * (main thread + screen capture thread)
     * it should be ok to not synchronize it
     */
    @Volatile
    var gameplayMode: String? = MODE_UNKNOWN
        private set

    override fun onLine(line: String) {
        Timber.v(line)

        val pattern = Pattern.compile(".*LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)")
        val matcher = pattern.matcher(line)
        if (matcher.matches()) {
            val prevMode = matcher.group(1)
            val currMode = matcher.group(2)

            mParsedMode = currMode
            if (!mReadingPreviousData) {
                /*
                 * do not trigger the mode changes for previous modes, it selects the arena deck at startup always
                 */
                mode = mParsedMode
                when (mode) {
                    MODE_DRAFT,
                    MODE_TOURNAMENT,
                    MODE_ADVENTURE,
                    MODE_FRIENDLY,
                    MODE_TAVERN_BRAWL
                    -> gameplayMode = mode
                }
            }
        }
    }

    override fun onPreviousDataRead() {
        mReadingPreviousData = false
        mode = mParsedMode
    }

    companion object {
        private var sParser: LoadingScreenParser? = null

        val MODE_TOURNAMENT = "TOURNAMENT"
        val MODE_DRAFT = "DRAFT"
        val MODE_GAMEPLAY = "GAMEPLAY"
        val MODE_COLLECTIONMANAGER = "COLLECTIONMANAGER"
        val MODE_PACKOPENING = "PACKOPENING"
        val MODE_FRIENDLY = "FRIENDLY"
        val MODE_ADVENTURE = "ADVENTURE"
        val MODE_HUB = "HUB"
        val MODE_TAVERN_BRAWL = "TAVERN_BRAWL"
        val MODE_UNKNOWN = "UNKNOWN"

        fun get(): LoadingScreenParser {
            if (sParser == null) {
                sParser = LoadingScreenParser()
            }
            return sParser!!
        }
    }
}
