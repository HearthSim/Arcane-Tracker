package net.arcanetracker.detector

import net.arcanetracker.ProcessHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PushTrainingDataTask : DefaultTask(){
    @TaskAction
    fun taskAction() {
        ProcessHelper.execOrFail("adb push ${project.rootDir.absolutePath}/training_data /sdcard")
    }
}
