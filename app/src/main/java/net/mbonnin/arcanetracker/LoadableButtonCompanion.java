package net.mbonnin.arcanetracker;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

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
        button.setClickable(false);
        button.setVisibility(View.INVISIBLE);
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
        button.setEnabled(true);
        button.setClickable(true);
        button.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        button.setOnClickListener(onClickListener);
        button.setText(text);
    }
}
