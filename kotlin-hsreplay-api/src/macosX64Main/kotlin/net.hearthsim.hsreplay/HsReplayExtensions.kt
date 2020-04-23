package net.hearthsim.hsreplay

import kotlinx.coroutines.*
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import platform.darwin.*
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
private val mainDispatcher: CoroutineDispatcher = object : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val queue = dispatch_get_main_queue()
        dispatch_async(queue) {
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


fun HsReplay.uploadCollectionWithCallback(collectionUploadData: CollectionUploadData,
                                 account_hi: String,
                                 account_lo: String,
                                 callback: (HsReplay.CollectionUploadResult) -> Unit) {
    GlobalScope.launch(mainDispatcher) {

        val r = uploadCollection(collectionUploadData, account_hi, account_lo)
        callback(r)
    }
}
