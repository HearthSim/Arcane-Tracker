package net.arcanetracker.detector

import android.util.Log
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import net.mbonnin.arcanetracker.detector.Parallel
import net.mbonnin.arcanetracker.detector.RRect
import java.util.concurrent.atomic.AtomicInteger

val MEDAL_RRECT = RRect(24.0, 82.0, 209.0, 92.0)

object Trainer {
    fun trainList(fileList: List<String>, extractFeatures: (ByteBufferImage) -> DoubleArray, allowMissing: Boolean = true): List<DoubleArray> {
        val i = AtomicInteger(0)

        return Parallel(fileList, {
            Log.d("Trainer", i.getAndIncrement().toString() + "/" + fileList.size + ":" + it)
            try {
                val byteBufferImage = ByteBufferImageFactory.create("/sdcard/training_data/ranks/" + it)
                extractFeatures(byteBufferImage)
            } catch (e: Exception) {
                if (allowMissing) {
                    DoubleArray(15, { 0.0 })
                } else {
                    throw e
                }
            }
        }).compute()
    }

    fun trainRanks(extractFeatures: (ByteBufferImage, RRect) -> DoubleArray): List<DoubleArray> {
        return trainList((0..25).map { String.format("/Medal_Ranked_%d.png", it) }, { bbi ->
            extractFeatures(bbi, MEDAL_RRECT)
        })
    }
}

