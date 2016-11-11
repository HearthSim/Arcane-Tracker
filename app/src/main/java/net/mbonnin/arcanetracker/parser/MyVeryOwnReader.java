package net.mbonnin.arcanetracker.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Created by martin on 11/11/16.
 */

/**
 * our own implementation of BuffereReader, etc... Mainly because I don't trust these classes to do the appropriate thing
 * when reading continuously from the end of file.... Maybe I'm wrong but I have to try to get a better understanding at all this
 */
public class MyVeryOwnReader {
    byte buffer[] = new byte[16*1024];
    FileInputStream inputStream;
    private int bufferMax;
    private int bufferRead;
    StringBuilder builder;

    public MyVeryOwnReader(File file) throws FileNotFoundException {
        inputStream = new FileInputStream(file);
        builder = new StringBuilder();
    }

    public void skip(long count) throws IOException {
        inputStream.skip(count);
        bufferMax = 0;
        bufferRead = 0;
        builder.setLength(0);
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This assumes ascii charset
     */
    public String readLine() throws IOException {
        while (true) {
            if (bufferRead == bufferMax) {
                int ret = inputStream.read(buffer);
                if (ret == -1) {
                    return null;
                } else if (ret == 0) {
                    Timber.e("0 bytes read");
                    return null;
                }
                bufferMax = ret;
                bufferRead = 0;
            }

            while (bufferRead < bufferMax) {
                byte b = buffer[bufferRead++];

                if (b == '\n') {
                    String line = builder.toString();
                    builder.setLength(0);
                    return line;
                } else {
                    builder.append((char)b);
                }
            }
        }
    }
}
