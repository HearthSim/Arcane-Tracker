package net.mbonnin.arcanetracker;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.view.View.GONE;

public class LoadableButtonCompanion {
    private final View mView;
    public final Button button;
    public final ProgressBar progressBar;

    public LoadableButtonCompanion(View claimView) {
        mView = claimView;
        button = (Button) claimView.findViewById(R.id.button);
        progressBar = (ProgressBar) claimView.findViewById(R.id.progressBar);
    }

    public <T> void startLoading(Observable<T> observable, Observer<T> observer) {
        button.setVisibility(GONE);
        progressBar.setVisibility(View.VISIBLE);
        observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<T>() {
                    @Override
                    public void onCompleted() {
                        progressBar.setVisibility(GONE);
                        button.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T t) {
                        observer.onNext(t);
                    }
                });
    }
}
