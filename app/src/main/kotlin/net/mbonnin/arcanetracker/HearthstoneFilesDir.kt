package net.mbonnin.arcanetracker

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URLEncoder

object HearthstoneFilesDir {
    private val hearthstoneExternalFilesPath = "/Android/data/com.blizzard.wtcg.hearthstone/files/"
    private val hsExternalFilesPath = Environment.getExternalStorageDirectory().path + hearthstoneExternalFilesPath

    private fun String.toId() = removePrefix("/").removeSuffix("/")
            .let {
                URLEncoder.encode(it)
            }

    private fun buildSafUri(tree: String, document: String? = null): Uri {
        val treeId = tree.toId()
        val documentId = document?.toId() ?: treeId

        val u = "content://com.android.externalstorage.documents/tree/primary%3A$treeId/document/primary%3A$documentId"
        return Uri.parse(u)!!
    }

    val treeUri = buildSafUri(hearthstoneExternalFilesPath)

    fun writeFile(name: String, contents: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = buildSafUri(hearthstoneExternalFilesPath)
            val directory = DocumentFile.fromTreeUri(ArcaneTrackerApplication.context, uri)!!
            val file = directory.listFiles().firstOrNull { it.name == name }

            val fileUri = if (file == null) {
                directory.createFile("plain/text", name)!!.uri
            } else {
                file.uri
            }

            ArcaneTrackerApplication.context.contentResolver.openOutputStream(fileUri)!!.use {
                it.writer().write(contents)
            }
        } else {
            File(hsExternalFilesPath + name).writeText(contents)
        }
    }

    // content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2Fcom.blizzard.wtcg.hearthstone%2Ffiles/document/primary%3AAndroid%2Fdata%2Fcom.blizzard.wtcg.hearthstone%2Ffiles%2FLog%2FLoadingScreen.log

    private fun logUri(log: String): Uri {
        val context = ArcaneTrackerApplication.context
        val logsDir = DocumentFile.fromTreeUri(context, buildSafUri(hearthstoneExternalFilesPath))!!
                .listFiles()
                .firstOrNull { it.name?.toLowerCase() == "logs" }

        check(logsDir != null) {
            "Cannot find logs dir"
        }
        val logFile = logsDir.listFiles().firstOrNull { it.name == log }
        if(logFile == null) {
            throw FileNotFoundException()
        }

        return logFile.uri
    }

    fun logOpen(log: String): InputStream {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val context = ArcaneTrackerApplication.context

            return context.contentResolver.openInputStream(logUri(log))!!
        } else {
            return File("$hsExternalFilesPath/Logs/$log").inputStream()
        }
    }

    fun logExists(log: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                logOpen(log).use { }
                true
            } catch (e: Exception) {
                false
            }
        } else {
            return File("$hsExternalFilesPath/Logs/$log").exists()
        }
    }

    /**
     * Same behaviour as [File.length]: returns 0L if the file does not exist
     */
    fun logSize(log: String): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                val context = ArcaneTrackerApplication.context
                val uri = logUri(log)
                val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null)!!
                cursor.moveToNext()
                cursor.getLong(0).also {
                    cursor.close()
                }
            } catch (e: Exception) {
                0L
            }
        } else {
            return File("$hsExternalFilesPath/Logs/$log").length()
        }
    }
}