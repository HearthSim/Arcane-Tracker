package net.mbonnin.arcanetracker

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.IOException

open class UpdateCardsJsonPlugin : Plugin<Project> {
    override fun apply(p0: Project) {
        p0.tasks.create("updateCardsJson") {
            it.doLast {downloadAllJson(p0.file("src/main/resources/"))}
        }
    }

    companion object {
        private fun downloadOneJson(lang: String, outputDir: File) {
            val url = "https://api.hearthstonejson.com/v1/latest/${lang}/cards.json"
            val response = OkHttpClient().newCall(Request.Builder().url(url).get().build()).execute()

            if (response.isSuccessful) {
                val outputFile = File(outputDir, "cards_${lang}.json")
                val bytes = response.body()?.bytes();

                if (bytes != null) {
                    outputFile.writeBytes(bytes)
                    System.out.println(url)
                } else {
                    throw IOException()
                }
            } else {
                System.out.println(response.body()?.string())
                //System.out.println(String(response.data))
            }
        }

        private fun downloadAllJson(outputDir: File) {
            Observable.fromArray("enUS", "frFR", "ptBR", "ruRU", "koKR", "zhCN", "zhTW", "esES")
                    .flatMap {
                        Observable.fromCallable({ downloadOneJson(it, outputDir) })
                                .subscribeOn(Schedulers.io())
                    }
                    .toList()
                    .blockingGet()
        }
    }
}