package net.mbonnin.arcanetracker;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

public class FileTree extends Timber.Tree {
    private static FileTree sTree;
    private File mFile;
    private BufferedWriter mWriter = null;

    public FileTree(Context context) {
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
                mWriter.write(s);
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

}
