package net.arcanetracker.detector

import android.Manifest
import android.support.test.rule.GrantPermissionRule
import com.squareup.moshi.Moshi
import net.mbonnin.arcanetracker.detector.Detector
import net.mbonnin.arcanetracker.detector.RankData
import okio.Okio
import org.junit.Rule
import org.junit.Test
import java.io.File


class GenerateRankData : BaseTest() {

    @Test
    fun generateRanks() {
        val data = RankData(
                RANKS = Trainer.trainRanks(Detector.Companion::extractHaar)
        )

        val adapter = Moshi.Builder().build().adapter(RankData::class.java)

        val sink = Okio.buffer(Okio.sink(File("/sdcard/rank_data.json")))

        sink.use {
            adapter.toJson(it, data)
        }
    }
}