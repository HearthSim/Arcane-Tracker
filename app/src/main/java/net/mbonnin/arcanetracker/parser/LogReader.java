package net.mbonnin.arcanetracker.parser;

import android.os.Handler;


import net.mbonnin.arcanetracker.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class LogReader implements Runnable {
    private final String mLog;
    private final Handler mHandler;
    private boolean mReadPreviousData;
    private boolean mCanceled;
    private LineConsumer mLineConsumer;
    private int mLastSeconds;

    interface LineConsumer {
        void onLine(String rawLine, int seconds, String line);
    }

    public LogReader(String log, LineConsumer lineConsumer, boolean readPreviousData) {
        mLineConsumer = lineConsumer;
        mLog = log;

        mHandler = new Handler();
        mReadPreviousData = readPreviousData;
        Thread thread = new Thread(this);
        thread.start();

    }

    public void cancel() {
        mCanceled = true;
    }

    private static int getSeconds(String time) {
        String a[] = time.split(":");
        if (a.length < 3) {
            Timber.e("bad time" + time);
            return 0;
        }

        int sec = 0;
        sec += Integer.parseInt(a[0]) * 3600;
        sec += Integer.parseInt(a[1]) * 60;
        sec += Float.parseFloat(a[2]);

        return sec;
    }

    private static String getTimeStr(int seconds) {
        int hours = seconds / 3600;
        seconds = seconds % 3600;
        int min = seconds / 60;
        seconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, min, seconds);
    }

    @Override
    public void run() {
        File file = new File(mLog);

        /**
         * stupid workaround for the debugger....
         * If we don't sleep here and put breakpoints in the parser code, it gets executed while the MainActivity is destroying and the OS kill us for ANR
         */
        if (Utils.isAppDebuggable()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long lastSize;
        MyVeryOwnReader myReader;
        while (!mCanceled) {
            /**
             * try to open file
             */
            try {
                myReader = new MyVeryOwnReader(file);
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Timber.e(e1);
                }
                continue;
            }

            String line = null;
            lastSize = file.length();

            Timber.e("initial file size = %d bytes", lastSize);
            if (!mReadPreviousData) {
                try {
                    Timber.e("skipping %d bytes");
                    myReader.skip(lastSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /**
                 * Next time we come, it is that the file the file has been truncated.
                 * Assume it has been truncated to 0 and it's safe to read all the previous data (not sure about that)
                 */
                mReadPreviousData = true;
            }

            Timber.e("start looping");
            while (!mCanceled) {

                long size = file.length();
                if (size < lastSize) {
                    /**
                     * somehow someone truncated the file... do what we can
                     */
                    String w = String.format("truncated file ? (%s) [%d -> %d]", mLog, lastSize, size);
                    Utils.reportNonFatal(new TruncatedFileException(w));
                    break;
                }
                try {
                    line = myReader.readLine();
                } catch (IOException e) {
                    Timber.e(e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        Timber.e(e1);
                    }
                    Timber.e("cannot read log file file" + file);
                    Utils.reportNonFatal(e);
                    break;
                }
                if (line == null) {
                    //wait until there is more of the file for us to read
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Timber.e(e);
                    }
                } else {
                    // parse the beginning of line: "D 15:24:25.6488220 "
                    if (line.length() < 19) {
                        Timber.e("invalid line: " + line);
                        return;
                    }


                    int seconds = getSeconds(line.substring(2, 10));
                    String finalLine = line.substring(19);
                    String rawLine = line;

                    if (seconds < mLastSeconds) {
                        Timber.e("Time going backwards ? %s < %s", getTimeStr(seconds), getTimeStr(mLastSeconds));
                        Timber.e("on " + mLog);
                    }
                    mLastSeconds = seconds;

                    mHandler.post(() -> mLineConsumer.onLine(rawLine, seconds, finalLine));
                }
            }

            myReader.close();
        }
    }
}
