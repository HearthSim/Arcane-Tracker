import org.gradle.api.Plugin
import org.gradle.api.Project

class ATPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("cardJson", DownloadCardsJsonTask::class.java)
    }
}