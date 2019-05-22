package net.mbonnin.arcanetracker.hslog

import timber.log.Timber
import java.util.regex.Pattern

/**
 * Created by martin on 11/7/16.
 */

class LoadingScreenParser {

    private var isOldData = true

    private var mParsedMode: String = MODE_UNKNOWN

    @Volatile
    var mode: String = MODE_UNKNOWN
        private set

    // the last mode before entering gameplay
    var gameplayMode: String = MODE_UNKNOWN
        private set

    fun process(rawLine: String, isOldData: Boolean) {
        Timber.v(rawLine)

        if (this.isOldData && !isOldData) {
            setModeInternal(mParsedMode)
            this.isOldData = true
        }
        val pattern = Pattern.compile(".*LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)")
        val matcher = pattern.matcher(rawLine)
        if (matcher.matches()) {
            //val prevMode = matcher.group(1)
            val currMode = matcher.group(2)

            mParsedMode = currMode
            if (!isOldData) {
                /*
                 * do not trigger the mode changes for previous modes, it selects the arena deck at startup always
                 */
                setModeInternal(mParsedMode)
            }
        }
    }

    private fun setModeInternal(parsedMode: String) {
        Timber.d("setModeInternal " + parsedMode)

        mode = parsedMode

        when (mode) {
            MODE_DRAFT,
            MODE_TOURNAMENT,
            MODE_ADVENTURE,
            MODE_FRIENDLY,
            MODE_TAVERN_BRAWL
            -> gameplayMode = mode
        }
    }

    companion object {
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

    }
}
