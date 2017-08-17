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
    private final Button button;
    private final ProgressBar progressBar;

    public LoadableButtonCompanion(View View) {
        mView = View;
        button = (Button) View.findViewById(R.id.button);
        progressBar = (ProgressBar) View.findViewById(R.id.progressBar);
    }

    public void setLoading() {
        button.setVisibility(GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public View view() {
        return mView;
    }

    public Button button() {
        return button;
    }

    public ProgressBar progressBar() {
        return progressBar;
    }
    public void setText(String text, View.OnClickListener onClickListener) {
        button.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        button.setOnClickListener(onClickListener);
        button.setText(text);
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
