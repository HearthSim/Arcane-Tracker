package net.hearthsim.hsreplay

import kotlinx.coroutines.CoroutineScope
import java.io.File


actual fun readResource(name: String): ByteArray {
    return File(System.getProperty("user.dir"), "src/commonTest/resources/$name").readBytes()
}

actual fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    kotlinx.coroutines.runBlocking {
        block()
    }
}