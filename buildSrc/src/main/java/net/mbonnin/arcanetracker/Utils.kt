package net.mbonnin.arcanetracker

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

fun InputStream.toFile(file: File) {
    use { input ->
        file.outputStream().use { input.copyTo(it) }
    }
}

fun String.httpGetToFile(file: File, skipIfExists:Boolean = false) {

    if (skipIfExists && file.exists()) {
        return
    }

    val response = OkHttpClient.Builder()
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            .newCall(Request.Builder()
                    .url(this)
                    .get()
                    .build())
            .execute()

    if (response.isSuccessful) {
        try {
            val r = response.body()?.byteStream()?.toFile(file)
            if (r == null) {
                throw Exception("cannot write file")
            }
        } catch (e: Exception) {
            if (file.exists()) {
                file.delete()
            }
            throw e
        }
    } else {
        System.out.println()
        throw Exception("HTTP error " + response.body()?.string())
    }
}