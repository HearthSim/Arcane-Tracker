package net.arcanetracker.detector

import androidx.test.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import net.mbonnin.arcanetracker.detector.Detector
import org.junit.Assert
import org.junit.Test
import timber.log.Timber

class TestRank : BaseTest() {

    @Test
    fun run() {
        testFile("pixel2xl.png", false, 6, 6)
        testFile("s8.jpg", false, 3, 4)
        testFile("shield.png", true, 3, 3)
    }

    private fun testFile(fileName: String, isTablet: Boolean, expectedPlayerRank: Int, expectedOpponentRank: Int) {
        val detector = Detector(InstrumentationRegistry.getTargetContext(), isTablet)

        Timber.d("testing $fileName [$expectedPlayerRank, $expectedOpponentRank]")
        val bbImage = ByteBufferImageFactory.create("/sdcard/arcane_tracker_extra/mulligans/$fileName")

        detector.prepareImage(bbImage)
        Assert.assertEquals(expectedPlayerRank, detector.detectPlayerRank(bbImage))
        Assert.assertEquals(expectedOpponentRank, detector.detectOpponentRank(bbImage))
    }
}