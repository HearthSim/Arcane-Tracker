package net.arcanetracker.detector

import net.arcanetracker.ProcessHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class UpdateRankDataTask : DefaultTask() {
    @TaskAction
    fun taskAction() {
        ProcessHelper.execOrFail("adb pull /sdcard/rank_data.json ${project.projectDir}/src/main/resources/rank_data.json")
    }
}
