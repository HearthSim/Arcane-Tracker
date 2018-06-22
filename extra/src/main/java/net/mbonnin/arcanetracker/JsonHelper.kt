package net.mbonnin.arcanetracker

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okio.Okio
import java.io.File
import java.lang.reflect.Type



object JsonHelper {
    private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    fun <T> decode(filePath: String, type: Type): T {
        val adapter = moshi.adapter<T>(type)
        val bufferedSource = Okio.buffer(Okio.source(File(filePath)))
        return bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }!! // <= not really sure if moshi can return null values since it usually throws exceptions
    }

    fun <T> encode(filePath: String, type: Type, o: T) {
        val cardDataMapAdapter = moshi.adapter<T>(type)
        val cardDataSink = Okio.buffer(Okio.sink(File(filePath)))
        cardDataSink.use {
            cardDataMapAdapter.toJson(it, o)
        }
    }
}
