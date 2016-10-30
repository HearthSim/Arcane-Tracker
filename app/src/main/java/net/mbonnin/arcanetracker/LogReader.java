package net.mbonnin.arcanetracker;

import android.os.Handler;

import com.google.firebase.crash.FirebaseCrash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class LogReader implements Runnable {
    private final InputStream mDebugInputStream = null;
    private final String mLog;
    private final Handler mHandler;
    private final boolean mReadPreviousData;
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
        File file = new File(MainActivity.HEARTHSTONE_FILES_DIR + "Logs/" + mLog + ".log");

        long lastSize;
        BufferedReader br;
        while (!mCanceled) {
            /**
             * try to open file
             */
            try {
                if (mDebugInputStream != null) {
                    br = new BufferedReader(new InputStreamReader(mDebugInputStream));
                } else {
                    br = new BufferedReader(new FileReader(file));
                }
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                continue;
            }

            String line = null;
            lastSize = file.length();

            if (mDebugInputStream == null && !mReadPreviousData) {
                /**
                 * consume all the previous line data
                 */
                while (!mCanceled) {
                    try {
                        line = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (line == null) {
                        break;
                    }

                }
            }
            while (!mCanceled) {

                long size = file.length();
                if (size < lastSize) {
                    /**
                     * somehow someone truncated the file... do what we can
                     */
                    String w = String.format("truncated file ? (%s)", mLog);
                    Timber.e(w);
                    FirebaseCrash.report(new Exception(w));
                    break;
                }
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    Timber.e("cannot read log file file" + file);
                    break;
                }
                if (line == null) {
                    //wait until there is more of the file for us to read
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    String finalLine = line;
                    mHandler.post(() -> mListener.onLine(finalLine));
                }
            }

            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
