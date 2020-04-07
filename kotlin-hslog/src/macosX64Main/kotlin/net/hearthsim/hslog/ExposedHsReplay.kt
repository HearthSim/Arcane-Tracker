package net.hearthsim.hslog

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.Preferences
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import kotlin.coroutines.CoroutineContext

class ExposedHsReplay(preferences: Preferences, val console: Console, analytics: Analytics, userAgent: String) : CoroutineScope {
    val hsReplay = HsReplay(
        preferences = preferences,
        console = console,
        analytics = analytics,
        userAgent = userAgent,
        clientId = "pk_live_IB0TiMMT8qrwIJ4G6eVHYaAi",
        clientSecret = "sk_live_20180308078UceCXo8qmoG72ExZxeqOW"
    )

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
        runBlocking {
            val r = hsReplay.uploadCollection(collectionUploadData, account_hi, account_lo)
            callback(when (r) {
                is HsReplay.CollectionUploadResult.Success -> Result.Success
                is HsReplay.CollectionUploadResult.Failure -> Result.Failure(r.code, r.throwable)
            })
        }
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + mainDispatcher
}