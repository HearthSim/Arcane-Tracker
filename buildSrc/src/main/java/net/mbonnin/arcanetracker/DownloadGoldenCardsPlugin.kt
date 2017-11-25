package net.mbonnin.arcanetracker

import com.google.gson.JsonParser
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors

open class DownloadGoldenCardsPlugin : Plugin<Project> {
    val SKIP_IF_EXISTS = true

    override fun apply(p0: Project) {
        p0.tasks.create("downloadGoldenCards") {
            it.doLast { Task(p0.file("golden_cards"), SKIP_IF_EXISTS).run() }
        }
    }

    class Task(val outputDir: File, val skipIfExist: Boolean) {
        fun run() {
            val scheduler = Schedulers.from(Executors.newFixedThreadPool(5))

            outputDir.mkdirs()
            System.out.println("Task::run")

            val url = "https://github.com/Shipow/searchstone/raw/master/import/out/algolia-hearthstone.json"
            val algoliaFile = File(url.substringAfterLast("/"))

            url.httpGetToFile(algoliaFile, skipIfExists = true)

            val root = JsonParser().parse(InputStreamReader(algoliaFile.inputStream()))

            Observable.fromIterable(root.asJsonArray)
                    .map { it.asJsonObject }
                    .filter {
                        if (it.get("lang").asString != "enUS") {
                            return@filter false
                        } else if (it.get("id") == null) {
                            System.out.println("no id for $it")
                            return@filter false
                        } else if (it.get("anim") == null) {
                            System.out.println("no anim for $it")
                            return@filter false
                        }
                        return@filter true
                    }
                    .map {
                        val id = it.get("id").asString
                        val goldenUrl = it.get("anim").asString

                        return@map {
                            System.out.println(id + " -> " + goldenUrl)
                            goldenUrl.httpGetToFile(File(outputDir, id + "." + goldenUrl.substringAfterLast(".")), true)
                        }
                    }
                    .map { Single.fromCallable(it) }
                    .flatMapSingle { it.observeOn(scheduler) }
                    .toList()
                    .blockingGet()
        }
    }
}

