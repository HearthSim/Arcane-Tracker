package net.mbonnin.arcanetracker

import com.google.gson.JsonParser
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.Executors

open class DownloadGoldenCardsPlugin : Plugin<Project> {

    override fun apply(p0: Project) {
        p0.tasks.create("downloadGoldenCards") {
            it.doLast { Task(p0.file("golden_cards")).run() }
        }
    }

    class Task(val outputDir: File) {
        val scheduler = Schedulers.from(Executors.newFixedThreadPool(5))

        fun run() {
            val url = "https://github.com/Shipow/searchstone/raw/master/import/out/algolia-hearthstone.json"
            val response = OkHttpClient().newCall(Request.Builder().url(url).get().build()).execute()

            val stream = response.body()?.charStream()

            if (response.isSuccessful && stream != null) {
                val root = JsonParser().parse(stream)

//                Observable.fromArray(root.asJsonArray.map { it.asJsonObject }
//                        .map {
//                            val id = it.get("id").asString
//                            val goldenUrl = it.get("anim").asString
//                            { downloadCard(outputDir, id, goldenUrl) }
//                        }
//                        .map { Single.fromCallable(it) }
//                        .map { it.subscribeOn(scheduler) }
//
//                ).just("")
//                        .flatMap { Observable.just("") }
//                        .toList()


            }
        }

        private fun downloadCard(outputDir: File, id: String?, goldenUrl: String?): Any {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

