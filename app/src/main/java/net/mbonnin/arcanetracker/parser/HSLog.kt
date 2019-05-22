package net.mbonnin.arcanetracker.parser

class HSLog {
    private val loadingScreenParser = LoadingScreenParser()

    fun processLoadingScreen(rawLine: String, isOldData: Boolean) {
        loadingScreenParser.process(rawLine, isOldData)
    }

    fun loadingScreenMode(): String {
        return loadingScreenParser.gameplayMode
    }

}