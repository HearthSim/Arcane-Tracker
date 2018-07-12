package net.arcanetracker.hsmodel

import org.gradle.api.Plugin
import org.gradle.api.Project

class HSModelPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("updateCardsJson", UpdateCardsJson::class.java)
    }
}