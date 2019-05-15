package net.mbonnin.arcanetracker.reader

import android.net.Uri
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.HideDetector
import net.mbonnin.arcanetracker.Utils
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException

class LogReader(private val mLog: String) : Runnable {
    private var oldData = true
    private var mCanceled: Boolean = false
    private lateinit var lineConsumer: (rawLine: String, isOldData: Boolean) -> Unit

    fun start(lineConsumer: (rawLine: String, isOldData: Boolean) -> Unit) {
        this.lineConsumer = lineConsumer
        val thread = Thread(this)
        thread.start()
    }

    fun cancel() {
        mCanceled = true
    }

    override fun run() {
        var lastSize: Long
        while (!mCanceled) {
            val fd = try {
                ArcaneTrackerApplication.get().contentResolver
                        .openFileDescriptor(
                                Uri.parse("content://com.blizzard.wtcg.hearthstone.exportedcontentprovider/Logs/${mLog}"), "r"
                        )
            } catch (e: java.lang.Exception) {
                Timber.e(e)
                null
            }

            if (fd == null) {
                // if the file does not exist, there is no previous data to read
                // this prevents a small race where we could miss the first bytes of data
                oldData = false
                try {
                    Thread.sleep(1000)
                } catch (e1: InterruptedException) {
                    Timber.e(e1)
                }
                continue
            }

            val myReader = FileInputStream(fd.fileDescriptor).bufferedReader()


            var line: String?
            lastSize = fd.statSize

            Timber.e("$mLog: start looping")
            while (!mCanceled) {
                val size = fd.statSize
                if (size < lastSize) {
                    /*
                     * somehow someone truncated the file... do what we can
                     */
                    val w = String.format("$mLog: truncated file ? [$lastSize -> $size]")
                    Timber.e(w)
                    break
                }

                try {
                    line = myReader.readLine()
                } catch (e: IOException) {
                    Timber.e(e)
                    try {
                        Thread.sleep(1000)
                    } catch (e1: InterruptedException) {
                        Timber.e(e1)
                    }

                    Utils.reportNonFatal(Exception("$mLog: cannot read log file $mLog", e))
                    break
                }

                if (line == null) {
                    if (oldData) {
                        /*
                         * we've reach the EOF, everything is new data now
                         */
                        Timber.e("$mLog: All old data read")
                        oldData = false
                    }

                    //wait until there is more of the file for us to read
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        Timber.e(e)
                    }

                } else {
                    Timber.e("$mLog: $line")
                    HideDetector.get().ping()
                    lineConsumer(line, oldData)
                }
            }

            myReader.close()
        }
    }
}
