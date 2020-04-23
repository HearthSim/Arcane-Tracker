package net.hearthsim.hsreplay

import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopRun
import platform.CoreFoundation.CFRunLoopStop
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.getcwd

actual fun readResource(name: String): ByteArray {
    val cwd = memScoped {
        val tmp = allocArray<ByteVar>(512)
        getcwd(tmp, 512.convert())
        tmp.toKString()
    }
    println("cwd=$cwd")

    val nsData = NSData.dataWithContentsOfURL(NSURL(fileURLWithPath = "$cwd/src/commonTest/resources/$name"))!!
    return nsData.bytes!!.readBytes(nsData.length.toInt())
}

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch(testDispatcher) {
        block()
        CFRunLoopStop(CFRunLoopGetCurrent())
    }

    CFRunLoopRun()
}