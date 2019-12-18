package net.hearthsim.hslog.hsreplay

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.Preferences
import net.hearthsim.hsreplay.model.new.CollectionUploadData

class ExposedHsReplay(preferences: Preferences, console: Console, analytics: Analytics, userAgent: String) {
    val hsReplay = HsReplay(preferences = preferences, console = console, analytics = analytics, userAgent = userAgent)

    fun setTokens(accessToken: String, refreshToken: String) {
        hsReplay.setTokens(accessToken, refreshToken)
    }

    sealed class Result {
        object Success: Result()
        class Failure(val e: Throwable): Result()
    }
    fun uploadCollectionWithCallback(collectionUploadData: CollectionUploadData,
                                     account_hi: String,
                                     account_lo: String,
                                     callback: (Result) -> Unit) {
        GlobalScope.launch {
            val r = hsReplay.uploadCollection(collectionUploadData, account_hi, account_lo)
            callback(when(r) {
                is HsReplay.CollectionUploadResult.Success -> Result.Success
                is HsReplay.CollectionUploadResult.Failure -> Result.Failure(r.e)

            })
        }
    }

}