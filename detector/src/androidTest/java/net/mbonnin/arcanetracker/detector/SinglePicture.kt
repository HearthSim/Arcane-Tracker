package net.mbonnin.arcanetracker.detector

import android.support.test.InstrumentationRegistry
import net.mbonnin.hsmodel.CardJson


class SinglePicture {
    @Test
    fun single() {
        CardJson.init("enUS")
        var detector = Detector(InstrumentationRegistry.getTargetContext(), false)



    }
}