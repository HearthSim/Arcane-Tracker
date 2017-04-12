package net.mbonnin.arcanetracker;

import android.content.Context;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

public class PicassoCardRequestHandler extends RequestHandler {
    private final Context context;
    private final OkHttpClient mOkHttpClient;
    private final String mLanguage;
    private HashMap<String, Callback> mPendingCallbacks = new HashMap<>();
    private File dir;

    public PicassoCardRequestHandler(Context context, OkHttpClient okHttpClient, File cacheDir) {
        this.context = context;
        mOkHttpClient = okHttpClient;
        dir = cacheDir;

        String locale = Locale.getDefault().getLanguage().toLowerCase();

        if (locale.contains("fr")) {
            mLanguage = "frfr";
        } else if (locale.contains("pt")) {
            mLanguage = "ptbr";
        } else if (locale.contains("ru")) {
            mLanguage = "ruru";
        } else {
            mLanguage = "enus";
        }
    }

    @Override
    public boolean canHandleRequest(Request data) {
        if (data.uri.getScheme().equals("card")) {
            return true;
        }

        return false;
    }

    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Callback implements Func0<Void> {
        private final String mName;
        public boolean done;
        String mUrl;

        public Callback(String url, String name) {
            mUrl = url;
            mName = name;
        }

        @Override
        public Void call() {
            okhttp3.Request r = new okhttp3.Request.Builder()
                    .url(mUrl)
                    .get()
                    .build();

            Response response;
            try {
                response = mOkHttpClient.newCall(r).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("bad response");
                }

                File f = new File(dir, mName + ".tmp");

                copyInputStreamToFile(response.body().byteStream(), f);

                f.renameTo(new File(dir, mName));

            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized (mPendingCallbacks) {
                mPendingCallbacks.remove(this);
            }

            synchronized (this) {
                done = true;
                notifyAll();
            }

            return null;
        }
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {

        String cardId = request.uri.getHost();

        String name = mLanguage + "_" + cardId + ".png";
        File f = new File(dir, name);

        synchronized(this) {
            if (f.exists()) {
                return new Result(new FileInputStream(f), Picasso.LoadedFrom.DISK);
            }
        }

        Callback callback = null;
        synchronized (mPendingCallbacks) {
            if (mPendingCallbacks.containsKey(name)) {
                callback = mPendingCallbacks.get(name);
            } else {
                String url = "http://vps208291.ovh.net/cards/" + mLanguage + "/" + cardId + ".png";

                callback = new Callback(url, name);
                mPendingCallbacks.put(name, callback);
                Async.start(callback)
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }
        }

        synchronized (callback) {
            while (!callback.done) {
                try {
                    callback.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized(this) {
            if (f.exists()) {
                return new Result(new FileInputStream(f), Picasso.LoadedFrom.NETWORK);
            } else {
                int id = context.getResources().getIdentifier(cardId.toLowerCase(), "drawable", context.getPackageName());
                if (id > 0) {
                    return new Result(context.getResources().openRawResource(id), Picasso.LoadedFrom.DISK);
                } else {
                    return null;
                }
            }
        }
    }
}
