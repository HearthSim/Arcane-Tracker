package net.mbonnin.arcanetracker;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import timber.log.Timber;

public class FileTree extends Timber.DebugTree {
    private static FileTree sTree;
    private File mFile;
    private BufferedWriter mWriter = null;

    SimpleDateFormat mDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);

    public FileTree(Context context) {
        super();
        mFile = new File(context.getExternalFilesDir(null), "ArcaneTracker.log");

        if (mFile.length() >= 5*1024*1024) {
            /**
             * try to make sure the file is not too big...
             */
            mFile.delete();
        }
    }

    public File getFile() {
        return mFile;
    }

    public void sync() {
        if (mWriter != null) {
            try {
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        super.log(priority, tag, message, t);

        if (mWriter == null) {
            /**
             * maybe we don't have permission yet, try later;
             */
            tryOpenWriter();
            if (mWriter == null) {
                return;
            }
        }

        int start = 0;
        String time = mDateFormat.format(Calendar.getInstance().getTime());
        while (start < message.length()) {
            int end = message.indexOf('\n', start);

            if (end == -1) {
                end = message.length();
            }

            String s;
            if (end == message.length()) {
                s = message.substring(start, end) + "\n";
            } else {
                s = message.substring(start, end + 1);
            }

            try {
                mWriter.write(time + " " + tag + " " + s);
            } catch (IOException e) {
                e.printStackTrace();
            }

            start = end + 1;
        }
    }

    private void tryOpenWriter() {
        try {
            mWriter = new BufferedWriter(new FileWriter(mFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileTree get() {
        if (sTree == null) {
            sTree = new FileTree(ArcaneTrackerApplication.getContext());
        }
        return sTree;
    }
}
