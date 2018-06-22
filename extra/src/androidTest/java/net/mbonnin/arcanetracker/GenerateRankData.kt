package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.detector.Detector
import net.mbonnin.arcanetracker.detector.RankData
import org.junit.Test


class GenerateRankData : BaseTest() {

    @Test
    fun generateRanks() {
        val data = RankData(
                RANKS = Trainer.trainRanks(Detector.Companion::extractHaar)
        )
        JsonHelper.encode("/sdcard/rank_data.json", RankData::class.java, data)
    }
}