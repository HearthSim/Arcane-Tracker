package net.mbonnin.plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class ButterKnifePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("hello") {
            val action = Action<String> { println(it) }

            println("Hello world")
        }
    }
}