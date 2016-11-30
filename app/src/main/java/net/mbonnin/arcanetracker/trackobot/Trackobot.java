package net.mbonnin.arcanetracker.trackobot;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.google.gson.Gson;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import io.paperdb.Paper;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by martin on 10/25/16.
 */

public class Trackobot {
    private static final java.lang.String KEY_USER = "USER";
    private static final java.lang.String KEY_PENDING_RESULT_DATA = "PENDING_RESULT_DATA";
    private static Trackobot sTrackobot;
    private final Service mService;
    private User mUser;
    private ArrayList<ResultData> pendingResultData;

    public static Trackobot get() {
        if (sTrackobot == null) {
            sTrackobot = new Trackobot();
        }

        return sTrackobot;
    }

    synchronized public void setUser(User user) {
        mUser = user;
        if (user == null) {
            Paper.book().delete(KEY_USER);
        } else {
            Paper.book().write(KEY_USER, mUser);
        }
    }

    public User getUser() {
        return mUser;
    }

    class ResultDataObserver implements Observer<ResultData> {
        public ResultData resultData;

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            String message;
            Context context = ArcaneTrackerApplication.getContext();
            if (e instanceof HttpException) {
                message = context.getString(R.string.trackobotHttpError, ((HttpException) e).code());
            } else if (e instanceof SocketTimeoutException) {
                message = context.getString(R.string.trackobotTimeout);
            } else if (e instanceof ConnectException) {
                message = context.getString(R.string.trackobotConnectError);
            } else if (e instanceof IOException) {
                message = context.getString(R.string.trackobotNetworkError);
            } else {
                message = context.getString(R.string.trackobotError);
            }
            Toast.makeText(ArcaneTrackerApplication.getContext(), message, Toast.LENGTH_LONG).show();
            Utils.reportNonFatal(e);
        }

        @Override
        public void onNext(ResultData resultData) {
            Context context = ArcaneTrackerApplication.getContext();
            Toast.makeText(ArcaneTrackerApplication.getContext(), context.getString(R.string.trackobotSuccess), Toast.LENGTH_LONG).show();
        }
    }

    public static File findTrackobotFile() {
        File dir = Environment.getExternalStorageDirectory();
        File files[] = dir.listFiles();
        for (File f: files) {
            if (f.isFile() &&f.getName().contains(".track-o-bot")) {
                return f;
            }
        }

        return null;
    }

    public static User parseTrackobotFile(File f) {
        try {
            User user = new User();
            StringBuilder builder = new StringBuilder();
            DataInputStream stream = new DataInputStream(new FileInputStream(f));
            int usernameLen = stream.readInt();
            for (int i = 0; i < usernameLen/2; i++) {
                builder.append((char)stream.readShort());
            }
            user.username = builder.toString();
            builder.setLength(0);
            int passwordLen = stream.readInt();
            for (int i = 0; i < passwordLen/2; i++) {
                builder.append((char)stream.readShort());
            }
            user.password = builder.toString();
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Trackobot() {

        mUser = Paper.book().read(KEY_USER);

        if (Utils.isAppDebuggable()) {
            mUser = new User();
            mUser.username = "bitter-void-terror-7444";
            mUser.password = "f762d37712";
            Paper.book().write(KEY_USER, mUser);
        }

        pendingResultData = Paper.book().read(KEY_PENDING_RESULT_DATA);
        if (pendingResultData == null) {
            pendingResultData = new ArrayList<>();
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();

                    Request.Builder requestBuilder = request.newBuilder();
                    String path = request.url().pathSegments().get(0);
                    HttpUrl.Builder urlBuilder = request.url().newBuilder();
                    if (path.startsWith("users")) {

                    } else {
                        urlBuilder.addQueryParameter("username", mUser.username);
                        requestBuilder.addHeader("Authorization", Credentials.basic(mUser.username, mUser.password));
                    }

                    requestBuilder.url(urlBuilder.build());
                    request = requestBuilder.build();
                    return chain.proceed(request);
                }).build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://trackobot.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(client)
                .build();

        mService = retrofit.create(Service.class);
    }

    public Service service() {
        return mService;
    }


    public static String getHero(int classIndex) {
        if (classIndex < 0 || classIndex >= Card.classNameList.length) {
            return "?";
        }
        return Card.classNameList[classIndex];
    }

    public void sendResult(ResultData resultData) {
        if (!Utils.isNetworkConnected()) {
            Timber.w("offline, sendResult later");
            pendingResultData.add(resultData);
            Paper.book().write(KEY_PENDING_RESULT_DATA, pendingResultData);
            return;
        }

        if (pendingResultData.size() > 10) {
            Utils.reportNonFatal(new Exception("Emptying Track-o-bot queue"));
            pendingResultData.clear();
        }

        while (!pendingResultData.isEmpty()) {
            ResultData pendingData = pendingResultData.remove(0);
            sendResultInternal(pendingData);
            Paper.book().write(KEY_PENDING_RESULT_DATA, pendingResultData);
        }

        sendResultInternal(resultData);
    }

    public void sendResultInternal(ResultData resultData) {
        Timber.w("sendResult");
        ResultDataObserver observer = new ResultDataObserver();
        observer.resultData = resultData;
        Trackobot.get().service().postResults(resultData).
                observeOn(AndroidSchedulers.mainThread()).subscribe(observer);

    }
}
