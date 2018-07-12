import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class DownloadCardsJsonTask: DefaultTask() {
    @TaskAction
    fun taskAction() {
        val client = OkHttpClient()
        val version = 25252
        val request = Request.Builder().url("https://api.hearthstonejson.com/v1/$version/all/cards.json").build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception()
        }


    }
}