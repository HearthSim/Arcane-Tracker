package net.arcanetracker.hsmodel

import net.arcanetracker.DownloadHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.wrapper.Download
import java.io.File

class HSModelPlugin: Plugin<Project> {
    companion object {
        val CARDS_JSON_VERSION = 25252 // increase here when a new patch happens
        val CARDS_JSON_URL = "https://api.hearthstonejson.com/v1/$CARDS_JSON_VERSION/all/cards.json"
        val CARDS_JSON_PATH = "src/main/resources/cards.json"
    }

    override fun apply(project: Project) {
        project.tasks.create("updateCardsJson", UpdateCardsJson::class.java)

        val cardsJsonFile = File(project.projectDir, CARDS_JSON_PATH)
        if (!cardsJsonFile.exists()) {
            DownloadHelper.download(CARDS_JSON_URL, cardsJsonFile)
        }
    }
}