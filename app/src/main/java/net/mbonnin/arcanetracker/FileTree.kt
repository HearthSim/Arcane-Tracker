package net.mbonnin.arcanetracker

import android.content.Context
import android.os.Build
import android.util.Log
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class FileTree(context: Context) : Timber.Tree() {
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


    private val MAX_LOG_LENGTH = 4000
    private val MAX_TAG_LENGTH = 23
    private val CALL_STACK_INDEX = 7
    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

    /**
     * Extract the tag which should be used for the message from the `element`. By default
     * this will use the class name without any anonymous class suffixes (e.g., `Foo$1`
     * becomes `Foo`).
     *
     *
     * Note: This will not be called if a [manual tag][.tag] was specified.
     */
    protected fun createStackElementTag(element: StackTraceElement): String? {
        var tag = element.className
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1)
        // Tag length limit was removed in API 24.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tag
        } else tag.substring(0, MAX_TAG_LENGTH)
    }

    fun getTag(): String? {
        // DO NOT switch this to Thread.getCurrentThread().getStackTrace(). The test will pass
        // because Robolectric runs them on the JVM but on Android the elements are different.
        val stackTrace = Throwable().stackTrace
        if (stackTrace.size <= CALL_STACK_INDEX) {
            throw IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements: are you using proguard?")
        }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX])
    }

    /**
     * Break up `message` into maximum-length chunks (if needed) and send to either
     * [Log.println()][Log.println] or
     * [Log.wtf()][Log.wtf] for logging.
     *
     * {@inheritDoc}
     */
    override fun log(priority: Int, unused: String?, message: String, t: Throwable?) {
        val tag = getTag()

        if (message.length < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(tag, message)
            } else {
                Log.println(priority, tag, message)
            }
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = Math.min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, part)
                } else {
                    Log.println(priority, tag, part)
                }
                i = end
            } while (i < newline)
            i++
        }

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
