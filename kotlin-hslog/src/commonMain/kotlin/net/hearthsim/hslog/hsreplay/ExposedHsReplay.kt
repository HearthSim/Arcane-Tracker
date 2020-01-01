package net.hearthsim.hslog.hsreplay

import kotlinx.coroutines.*
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hslog.mainDispatcher
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.Preferences
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import kotlin.coroutines.CoroutineContext

class ExposedHsReplay(preferences: Preferences, val console: Console, analytics: Analytics, userAgent: String) : CoroutineScope {
    val hsReplay = HsReplay(preferences = preferences, console = console, analytics = analytics, userAgent = userAgent)

    fun setTokens(accessToken: String, refreshToken: String) {
        hsReplay.setTokens(accessToken, refreshToken)
    }

    sealed class Result {
        object Success : Result()
        class Failure(val code: Int, val throwable: Throwable) : Result()
    }

    fun uploadCollectionWithCallback(collectionUploadData: CollectionUploadData,
                                     account_hi: String,
                                     account_lo: String,
                                     callback: (Result) -> Unit) {
        launch {
            val r = hsReplay.uploadCollection(collectionUploadData, account_hi, account_lo)
            callback(when (r) {
                is HsReplay.CollectionUploadResult.Success -> Result.Success
                is HsReplay.CollectionUploadResult.Failure -> Result.Failure(r.code, r.throwable)
            })
        }
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + mainDispatcher
}