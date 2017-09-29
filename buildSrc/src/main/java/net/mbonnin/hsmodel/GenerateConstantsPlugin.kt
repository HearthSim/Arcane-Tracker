package net.mbonnin.hsmodel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

open class GenerateConstantsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val generatedSourceDir = project.file("./build/generated/source/").absoluteFile
        val jsonFile = project.file("./src/main/resources/cards_enUS.json")

        val task = project.tasks.create("generateCardIds", GenerateConstantsTask::class.java) {
            it.inputFile = jsonFile
            it.outputDir = generatedSourceDir
        }

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main").java.srcDir(generatedSourceDir)
        project.tasks.getByName("compileKotlin").dependsOn(task)

        task.inputs.file(jsonFile)
        task.outputs.dir(generatedSourceDir)
    }
}
