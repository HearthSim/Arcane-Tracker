package net.mbonnin.arcanetracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.io.FileSystem;
import okio.Okio;
import okio.Sink;
import timber.log.Timber;

public class PicassoCardRequestHandler extends RequestHandler {
    private static final int VERSION = 1;
    private static final int ENTRY_COUNT = 1;
    private static PicassoCardRequestHandler sHandler;
    DiskLruCache cache;

    private PicassoCardRequestHandler() {
        File file = new File(ArcaneTrackerApplication.getContext().getCacheDir(), "cardsCache");
        cache = DiskLruCache.create(FileSystem.SYSTEM, file, VERSION, ENTRY_COUNT, 250 * 1024*1024);

        if (Settings.get(Settings.PICASSO_CARD_REQUEST_HANDLER_VERSION, VERSION) != VERSION) {
            try {
                cache.evictAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canHandleRequest(Request data) {
        if (data.uri.getScheme().equals("card")) {
            return true;
        }

        return false;
    }

    private static String getKey(String cardId, String langKey) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cardId.length(); i++) {
            char c = cardId.charAt(i);

            if (c >= 'A' && c <= 'Z') {
                builder.append(Character.toLowerCase(c));
            } else if (c >= '0' & c <= '9') {
                builder.append(c);
            } else if (c >= 'a' && c <= 'z') {
                builder.append(c);
            }
        }

        /**
         * XXX: this might become a bit wrong
         */
        builder.append("_");
        builder.append(langKey);
        return builder.toString();
    }
    @Override
    public Result load(Request request, int networkPolicy) throws IOException {

        String cardId = request.uri.getPath().substring(1);
        String langKey = request.uri.getHost();
        Picasso.LoadedFrom loadedFrom = Picasso.LoadedFrom.DISK;

        InputStream inputStream = null;
        if (true) {
            synchronized (cache) {
                try {
                    DiskLruCache.Snapshot snapshot = cache.get(getKey(cardId, langKey));
                    if (snapshot != null) {
                        inputStream = Okio.buffer(snapshot.getSource(0)).inputStream();
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        if (inputStream == null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                CardRenderer.get().renderCard(cardId, outputStream);
            } catch (Exception e) {
                Timber.e(e);
                return null;
            }
            byte bytes[] = outputStream.toByteArray();

            synchronized (cache) {
                DiskLruCache.Editor editor = cache.edit(getKey(cardId, langKey));
                if (editor != null) {
                    Sink sink = editor.newSink(0);
                    OutputStream os = Okio.buffer(sink).outputStream();
                    os.write(bytes);
                    os.close();
                    editor.commit();
                }
            }
            inputStream = new ByteArrayInputStream(bytes);
            outputStream.close();
            loadedFrom = Picasso.LoadedFrom.NETWORK;
        }

        Result result;
        Bitmap b = BitmapFactory.decodeStream(inputStream);
        if (b != null) {
            result =  new Result(b, loadedFrom);
        } else {
            throw new IOException();
        }
        inputStream.close();

        return result;
    }

    public static PicassoCardRequestHandler get() {
        if (sHandler == null) {
            sHandler = new PicassoCardRequestHandler();
        }

        return sHandler;
    }

    public void resetCache() {
        synchronized (cache) {
            try {
                cache.evictAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
