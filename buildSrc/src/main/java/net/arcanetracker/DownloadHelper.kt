package net.arcanetracker

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object DownloadHelper {
    fun download(url: String, file: File) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()

        file.parentFile.mkdirs()

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