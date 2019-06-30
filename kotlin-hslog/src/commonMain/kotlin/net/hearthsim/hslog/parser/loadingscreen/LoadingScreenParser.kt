package net.hearthsim.hslog.parser.loadingscreen

import net.hearthsim.console.Console
import kotlin.jvm.Volatile

class LoadingScreenParser(val console: Console) {

    private var isOldData = true

    private var mParsedMode: String = MODE_UNKNOWN

    @Volatile
    var mode: String = MODE_UNKNOWN
        private set

    // the last mode before entering gameplay
    var gameplayMode: String = MODE_UNKNOWN
        private set

    fun process(rawLine: String, isOldData: Boolean) {
        console.debug(rawLine)

        if (this.isOldData && !isOldData) {
            setModeInternal(mParsedMode)
            this.isOldData = true
        }
        val pattern = Regex(".*LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)")
        val matcher = pattern.matchEntire(rawLine)
        if (matcher != null) {
            //val prevMode = matcher.group(1)
            val currMode = matcher.groupValues[2]

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
        console.debug("setModeInternal $parsedMode")

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
