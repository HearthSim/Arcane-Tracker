package net.mbonnin.arcanetracker.detector

import android.support.test.InstrumentationRegistry
import org.junit.Test

class TestArena {
    @Test
    fun detectHeroes() {
        val byteBufferImage = inputStreamToByteBufferImage(javaClass.getResourceAsStream("/tests/arena_choices/heroes.png"))
        Detector(InstrumentationRegistry.getTargetContext(), false).detectArena(byteBufferImage)

    }
}