package net.arcanetracker.detector

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ATDetectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("pushTrainingData", PushTrainingDataTask::class.java)
        val task = project.tasks.create("updateRankData", UpdateRankDataTask::class.java)
        task.dependsOn("connectedDebugAndroidTest")
    }
}