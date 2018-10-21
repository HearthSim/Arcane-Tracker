package net.arcanetracker.app

import org.gradle.api.Plugin
import org.gradle.api.Project
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
    }
}