package net.mbonnin.arcanetracker.parser

import android.Manifest
import android.content.pm.PackageManager
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.QuitDetector
import net.mbonnin.arcanetracker.Utils
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class LogReader @JvmOverloads constructor(private val mLog: String, val mLineConsumer: LineConsumer, private var mSkipPreviousData: Boolean = false) : Runnable {
    private var mPreviousDataRead = false
    private var mCanceled: Boolean = false

    interface LineConsumer {
        fun onLine(rawLine: String)
        fun onPreviousDataRead()
    }

    init {

        val thread = Thread(this)
        thread.start()

    }

    fun cancel() {
        mCanceled = true
    }

    override fun run() {
        var lastSize: Long
        while (!mCanceled) {
            var myReader: MyVeryOwnReader? = null
            val file = File(Utils.hsExternalDir + "/Logs/" + mLog)

            /*
             * try to open file
             */
            try {
                myReader = MyVeryOwnReader(file)
            } catch (ignored: FileNotFoundException) {
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
            lastSize = file.length()

            Timber.e("%s: initial file size = %d bytes", mLog, lastSize)
            if (mSkipPreviousData) {
                try {
                    Timber.e("%s: skipping %d bytes", mLog, lastSize)
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

            Timber.e("%s: start looping", mLog)
            while (!mCanceled) {

                val size = file.length()
                if (size < lastSize) {
                    /*
                     * somehow someone truncated the file... do what we can
                     */
                    val w = String.format("%s: truncated file ? [%d -> %d]", mLog, lastSize, size)
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
                    QuitDetector.get().ping()
                    mLineConsumer.onLine(line)
                }
            }

            myReader.close()
        }
    }

    private fun previousDataConsumed() {
        mPreviousDataRead = true
        mLineConsumer.onPreviousDataRead()
    }

    class LogLine {
        var level: String? = null
        lateinit var line: String
        var method: String? = null
        var seconds: Int = 0
    }

    companion object {

        private fun getSeconds(time: String): Int {
            val a = time.split(":")
            if (a.size < 3) {
                Timber.e("bad time" + time)
                return 0
            }

            var sec = 0
            sec += Integer.parseInt(a[0]) * 3600
            sec += Integer.parseInt(a[1]) * 60
            sec += java.lang.Float.parseFloat(a[2]).toInt()

            return sec
        }

        private fun getTimeStr(seconds: Int): String {
            var seconds = seconds
            val hours = seconds / 3600
            seconds = seconds % 3600
            val min = seconds / 60
            seconds = seconds % 60

            return String.format("%02d:%02d:%02d", hours, min, seconds)
        }

        fun parseLine(line: String): LogLine? {
            val logLine = LogLine()

            //D 19:48:03.8108410 GameState.DebugPrintPower() -     Player EntityID=3 PlayerID=2 GameAccountId=redacted
            val s = line.split(" ")
            if (s.size < 3) {
                Timber.e("invalid line: %s", line)
                return null
            }

            logLine.level = s[0]
            try {
                logLine.seconds = getSeconds(s[1])
            } catch (e: NumberFormatException) {
                Timber.e("bad time: %s", line)
                return null
            }

            logLine.method = s[2]

            if (s.size == 3) {
                logLine.line = ""
                return logLine
            } else {
                if ("-" != s[3]) {
                    Timber.e("missing -: %s", line)
                    return null
                }
            }

            val start = line.indexOf("-")
            if (start >= line.length - 2) {
                Timber.e("empty line: %s", line)
                return null
            }
            logLine.line = line.substring(start + 2)


            return logLine
        }
    }
}
