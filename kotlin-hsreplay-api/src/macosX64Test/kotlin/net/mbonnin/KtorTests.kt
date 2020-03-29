package net.mbonnin

import kotlinx.coroutines.*
import net.hearthsim.analytics.DefaultAnalytics
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.Preferences
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import platform.CoreFoundation.CFRunLoopRun
import platform.darwin.*
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test

@OptIn(InternalCoroutinesApi::class)
class KtorTests {
    private val mainDispatcher: CoroutineDispatcher = object : CoroutineDispatcher(), Delay {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val queue = dispatch_get_main_queue()
            println("dispatcher")
            dispatch_async(queue) {
                println("dispatcher dispatched")
                block.run()
            }
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            val queue = dispatch_get_main_queue()

            val time = dispatch_time(DISPATCH_TIME_NOW, (timeMillis * NSEC_PER_MSEC.toLong()))
            dispatch_after(time, queue) {
                with(continuation) {
                    resumeUndispatched(Unit)
                }
            }
        }
    }

    class DummyPreferences : Preferences {
        override fun getBoolean(key: String): Boolean? {
            return false
        }

        override fun getString(key: String): String? {
            return null
        }

        override fun putBoolean(key: String, value: Boolean?) {
        }

        override fun putString(key: String, value: String?) {
        }

    }

    @Test
    fun testKtor() {
        println("Tests KTor")

        GlobalScope.launch(mainDispatcher) {
            val hsReplay = HsReplay(console = DefaultConsole(),
                userAgent = "tests",
                analytics = DefaultAnalytics(),
                preferences = DummyPreferences(),
                clientId = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP",
                clientSecret = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
            )

            hsReplay.setTokens("", "")

            runBlocking {
                val uploadData = CollectionUploadData(
                    collection = mapOf(
                        "52295" to listOf(2, 1),
                        "149" to listOf(2, 1)
                    ),
                    cardbacks = emptyList(),
                    dust = 289,
                    favoriteCardback = null,
                    favoriteHeroes = emptyMap(),
                    gold = 2349
                )
                val result = hsReplay.uploadCollection(
                    collectionUploadData = uploadData,
                    account_hi = "144115198130930503",
                    account_lo = "27472745"
                )

                if (result is HsReplay.CollectionUploadResult.Failure) {
                    throw (result.throwable)
                }
            }
        }

        val queue = dispatch_get_main_queue()
        println("dispatch")
        dispatch_async(queue) {
            println("Hello Martin")
        }

        CFRunLoopRun()
    }
}