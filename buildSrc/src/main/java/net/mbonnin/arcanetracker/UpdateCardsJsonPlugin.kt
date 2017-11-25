package net.mbonnin.arcanetracker

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

open class UpdateCardsJsonPlugin : Plugin<Project> {
    override fun apply(p0: Project) {
        p0.tasks.create("updateCardsJson") {
            it.doLast {downloadAllJson(p0.file("src/main/resources/"))}
        }
    }

    companion object {
        private fun downloadOneJson(lang: String, outputDir: File) {
            val outputFile = File(outputDir, "cards_${lang}.json")
            val url = "https://api.hearthstonejson.com/v1/latest/${lang}/cards.json"

            url.httpGetToFile(outputFile)
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