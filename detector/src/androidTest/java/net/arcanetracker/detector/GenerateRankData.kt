package net.arcanetracker.detector

import kotlinx.serialization.json.Json
import net.mbonnin.arcanetracker.detector.Detector
import net.mbonnin.arcanetracker.detector.RankData
import net.mbonnin.arcanetracker.detector.SerializableRankData
import net.mbonnin.arcanetracker.detector.serializable
import org.junit.Test
import java.io.File


class GenerateRankData : BaseTest() {

    @Test
    fun generateRanks() {
        val data = RankData(
                RANKS = Trainer.trainRanks(Detector.Companion::extractHaar)
        )

        File("/sdcard/rank_data.json").bufferedWriter().use {
            it.write(Json.nonstrict.stringify(SerializableRankData.serializer(), data.serializable()))
        }
    }
}