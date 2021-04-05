package net.mbonnin.arcanetracker.reader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.HearthstoneFilesDir
import net.mbonnin.arcanetracker.HideDetector
import net.mbonnin.arcanetracker.Utils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class LogReader(private val mLog: String, private var mSkipPreviousData: Boolean = false) : Runnable {
    private var mPreviousDataRead = false
    private var mCanceled: Boolean = false
    private lateinit var lineConsumer: LineConsumer

    fun start(lineConsumer: LineConsumer) {
        this.lineConsumer = lineConsumer
        val thread = Thread(this)
        thread.start()
    }

    private fun log(message: String) {
        if (mLog == "Power.log") {
            Timber.d(message)
        }
    }

    interface LineConsumer {
        fun onLine(rawLine: String)
        fun onPreviousDataRead()
    }

    fun cancel() {
        mCanceled = true
    }

    override fun run() {
        var lastSize: Long
        while (!mCanceled) {
            var myReader: MyVeryOwnReader?

            /*
             * try to open file
             */
            try {
                myReader = MyVeryOwnReader(HearthstoneFilesDir.logOpen(mLog))
            } catch (ignored: Exception) {
                if (mSkipPreviousData) {

                    if (ArcaneTrackerApplication.get().checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ArcaneTrackerApplication.get().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // we don't have permissions to read external storage yet
                    } else {
                        // if the file does not exist, there is no previous data to read
                        // this prevents a small race where we could miss the first bytes of data
                        previousDataConsumed()
                        mSkipPreviousData = false
                    }
                }

                //e.printStackTrace();
                try {
                    Thread.sleep(1000)
                } catch (e1: InterruptedException) {
                    Timber.e(e1)
                }

                continue
            }

            var line: String?
            lastSize = HearthstoneFilesDir.logSize(mLog)

            log("initial file size = $lastSize bytes")
            if (mSkipPreviousData) {
                try {
                    log("skipping $lastSize bytes")
                    myReader.skip(lastSize)
                } catch (e: IOException) {
                    Timber.e(e)
                }

                /*
                 * Next time we come, it is that the file the file has been truncated.
                 * Assume it has been truncated to 0 and it's safe to read all the previous data (not sure about that)
                 */
                mSkipPreviousData = false
                previousDataConsumed()
            }

            log("start looping")
            while (!mCanceled) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val size = HearthstoneFilesDir.logSize(mLog)
                    if (size < lastSize) {
                        /*
                     * somehow someone truncated the file... do what we can
                     */
                        val w = String.format("%s: truncated file ? [%d -> %d]", mLog, lastSize, size)
                        Timber.e(w)
                        break
                    }
                }
                try {
                    line = myReader.readLine()
                    //log(line ?: "null")
                } catch (e: IOException) {
                    Timber.e(e)
                    try {
                        Thread.sleep(1000)
                    } catch (e1: InterruptedException) {
                        Timber.e(e1)
                    }

                    Timber.e("%s: cannot read log file", mLog)
                    Utils.reportNonFatal(Exception("cannot read log file " + mLog, e))
                    break
                }

                if (line == null) {
                    if (!mPreviousDataRead) {
                        /*
                         * we've reach the EOF, everything is new data now
                         */
                        previousDataConsumed()
                    }

                    //wait until there is more of the file for us to read
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        Timber.e(e)
                    }

                } else {
                    HideDetector.get().ping()
                    lineConsumer.onLine(line)
                }
            }

            myReader.close()
        }
    }

    private fun previousDataConsumed() {
        mPreviousDataRead = true
        lineConsumer.onPreviousDataRead()
    }
}
