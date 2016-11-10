package net.mbonnin.arcanetracker.parser;

import android.os.Handler;


import net.mbonnin.arcanetracker.MainActivity;
import net.mbonnin.arcanetracker.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class LogReader implements Runnable {
    private final String mLog;
    private final Handler mHandler;
    private boolean mReadPreviousData;
    private boolean mCanceled;
    private Listener mListener;

    interface Listener {
        void onLine(String line);
    }

    public LogReader(String log, Listener listener, boolean readPreviousData) {
        mListener = listener;
        mLog = log;

        mHandler = new Handler();
        mReadPreviousData = readPreviousData;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void cancel() {
        mCanceled = true;
    }

    @Override
    public void run() {
        File file = new File(mLog);

        /**
         * stupid workaround...
         */
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long lastSize;
        BufferedReader br;
        while (!mCanceled) {
            /**
             * try to open file
             */
            try {
                br = new BufferedReader(new FileReader(file));
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

            if (!mReadPreviousData) {
                /**
                 * consume all the previous line data
                 */
                while (!mCanceled) {
                    try {
                        line = br.readLine();
                    } catch (IOException e) {
                        Timber.e(e);
                    }
                    if (line == null) {
                        break;
                    }
                }

                /**
                 * assume the file has been truncated to 0 and it's safe to read all the previous data (not sure about that)
                 */
                mReadPreviousData = true;
            }
            while (!mCanceled) {

                long size = file.length();
                if (size < lastSize) {
                    /**
                     * somehow someone truncated the file... do what we can
                     */
                    String w = String.format("truncated file ? (%s) [%d -> %d]", mLog, lastSize, size);
                    Utils.reportNonFatal(new Exception(w));
                    break;
                }
                try {
                    line = br.readLine();
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
                    String finalLine = line;
                    mHandler.post(() -> mListener.onLine(finalLine));
                }
            }

            try {
                br.close();
            } catch (IOException e) {
                Timber.e(e);
            }
        }
    }
}
