package net.mbonnin.arcanetracker

import android.content.Context
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileTree(context: Context) : Timber.DebugTree() {
    val file: File
    private var mWriter: BufferedWriter? = null

    internal var mDateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH)

    init {
        file = File(context.getExternalFilesDir(null), "ArcaneTracker.log")

        if (file.length() >= 5 * 1024 * 1024) {
            /**
             * try to make sure the file is not too big...
             */
            file.delete()
        }
    }

    fun sync() {
        if (mWriter != null) {
            try {
                mWriter!!.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        if (mWriter == null) {
            /**
             * maybe we don't have permission yet, try later;
             */
            tryOpenWriter()
            if (mWriter == null) {
                return
            }
        }

        var start = 0
        val time = mDateFormat.format(Calendar.getInstance().time)
        while (start < message.length) {
            var end = message.indexOf('\n', start)

            if (end == -1) {
                end = message.length
            }

            val s: String
            if (end == message.length) {
                s = message.substring(start, end) + "\n"
            } else {
                s = message.substring(start, end + 1)
            }

            try {
                mWriter!!.write("$time $tag $s")
            } catch (e: IOException) {
                e.printStackTrace()
            }

            start = end + 1
        }
    }

    private fun tryOpenWriter() {
        try {
            mWriter = BufferedWriter(FileWriter(file, true))
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {
        private var sTree: FileTree? = null

        fun get(): FileTree {
            if (sTree == null) {
                sTree = FileTree(ArcaneTrackerApplication.context)
            }
            return sTree!!
        }
    }
}
