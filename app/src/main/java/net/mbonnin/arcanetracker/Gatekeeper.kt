package net.mbonnin.arcanetracker

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.subjects.BehaviorSubject
import timber.log.Timber

object Gatekeeper {

    var root = JsonObject()
    val behaviorSubject = BehaviorSubject.create<Unit>()

    fun init() {
        Single.fromCallable {
            val client = OkHttpClient()

            val request = Request.Builder()
                    .url("https://arcanetracker.com/config.json")
                    .get()
                    .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body()!!.string()

                    return@fromCallable JsonParser().parse(json).asJsonObject
                } else {
                    throw Exception("bad response: ${response.code()}")
                }
            } catch (e: Exception) {
                throw e
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({ it ->
                    root = it
                    behaviorSubject.onNext(Unit)
                }, {
                    Timber.e(it)
                })
    }
}