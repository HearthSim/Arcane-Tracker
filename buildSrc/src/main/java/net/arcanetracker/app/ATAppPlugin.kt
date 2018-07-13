package net.arcanetracker.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.impldep.aQute.lib.io.IO.outputStream
import java.io.File

open class ATAppPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("extractUnityFiles", ExtractUnityFilesTask::class.java)

        val googleServicesFile = File(project.projectDir, "google-services.json")
        val googleServicesMockFile = File(project.projectDir, "google-services.json.mock")

        if (!googleServicesFile.exists()) {
            System.out.println("using mock google-services.json")
            googleServicesMockFile.copyTo(googleServicesFile)
        }

        val belwe = File(project.projectDir, "src/main/res/font/belwe_bold.ttf")
        val franklin = File(project.projectDir, "src/main/res/font/franklin_gothic.ttf")

        if (!belwe.exists()) {
            download("https://hearthsim.net/static/fonts/belwefs_extrabold_macroman/Belwe-ExtraBold-webfont.ttf", belwe)
        }
        if (!franklin.exists()) {
            download("https://hearthsim.net/static/fonts/franklingothicfs_mediumcondensed_macroman/franklingothic-medcd-webfont", franklin)
        }
    }

    private fun download(url: String, file: File) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("cannot download $url: ${response.code()}")
        }

        response.body()!!.byteStream().use {inputStream ->
            file.outputStream().use {
                inputStream.copyTo(it)
            }
        }

    }
}