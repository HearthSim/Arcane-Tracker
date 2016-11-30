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
    private boolean mPreviousDataRead = false;
    private boolean mCanceled;
    private LineConsumer mLineConsumer;
    private boolean mSkipPreviousData;

    interface LineConsumer {
        void onLine(String line);
        void onPreviousDataRead();
    }

    public LogReader(String log, LineConsumer lineConsumer) {
        this(log, lineConsumer, false);
    }
    public LogReader(String log, LineConsumer lineConsumer, boolean skipPreviousData) {
        mLineConsumer = lineConsumer;
        mSkipPreviousData = skipPreviousData;
        mLog = log;

        mHandler = new Handler();
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
        long lastSize;
        while (!mCanceled) {
            MyVeryOwnReader myReader = null;
            File file = new File(Utils.getHSExternalDir() + "/Logs/" + mLog);

            /*
             * try to open file
             */
            try {
                myReader = new MyVeryOwnReader(file);
            } catch (FileNotFoundException ignored) {
                /*
                 * if the file does not exist, there is no previous data to read
                 */
                previousDataConsumed();
                mSkipPreviousData = false;

                //e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Timber.e(e1);
                }
                continue;
            }

            String line;
            lastSize = file.length();

            Timber.e("%s: initial file size = %d bytes", mLog, lastSize);
            if (mSkipPreviousData) {
                try {
                    Timber.e("%s: skipping %d bytes", mLog, lastSize);
                    myReader.skip(lastSize);
                } catch (IOException e) {
                    Timber.e(e);
                }

                /*
                 * Next time we come, it is that the file the file has been truncated.
                 * Assume it has been truncated to 0 and it's safe to read all the previous data (not sure about that)
                 */
                mSkipPreviousData = false;
                previousDataConsumed();
            }

            Timber.e("%s: start looping", mLog);
            while (!mCanceled) {

                long size = file.length();
                if (size < lastSize) {
                    /**
                     * somehow someone truncated the file... do what we can
                     */
                    String w = String.format("%s: truncated file ? [%d -> %d]", mLog, lastSize, size);
                    Timber.e(w);
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
                    Timber.e("%s: cannot read log file file", mLog);
                    Utils.reportNonFatal(e);
                    break;
                }
                if (line == null) {
                    if (!mPreviousDataRead) {
                        /**
                         * we've reach the EOF, everything is new data now
                         */
                        previousDataConsumed();
                    }

                    //wait until there is more of the file for us to read
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Timber.e(e);
                    }
                } else {
                    String finalLine = line;
                    mHandler.post(() -> mLineConsumer.onLine(finalLine));
                }
            }

            myReader.close();
        }
    }

    private void previousDataConsumed() {
        mPreviousDataRead = true;
        mHandler.post(() -> mLineConsumer.onPreviousDataRead());
    }

    static class LogLine {
        public String level;
        public String line;
        public String method;
        public int seconds;
    }

    public static LogLine parseLine(String line) {
        LogLine logLine = new LogLine();

        //D 19:48:03.8108410 GameState.DebugPrintPower() -     Player EntityID=3 PlayerID=2 GameAccountId=[hi=144115198130930503 lo=19268725]
        String[] s = line.split(" ");
        if (s.length < 3) {
            Timber.e("invalid line: %s", line);
            return null;
        }

        logLine.level = s[0];
        try {
            logLine.seconds = getSeconds(s[1]);
        } catch (NumberFormatException e) {
            Timber.e("bad time: %s", line);
            return null;
        }

        logLine.method = s[2];

        if (s.length == 3) {
            logLine.line = "";
            return logLine;
        } else {
            if (!"-".equals(s[3])) {
                Timber.e("missing -: %s", line);
                return null;
            }
        }

        int start = line.indexOf("-");
        if (start >= line.length() - 2) {
            Timber.e("empty line: %s", line);
            return null;
        }
        logLine.line = line.substring(start + 2);


        return logLine;
    }
}
