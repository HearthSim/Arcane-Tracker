package net.mbonnin.arcanetracker;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static android.view.View.GONE;

public class MainViewCompanion {
    private static DeckCompanion sOpponentCompanion;
    private final ViewManager.Params mParams;
    private static DeckCompanion sPlayerCompanion;
    private ViewManager mViewManager;

    private View playerView;
    private View opponentView;
    private View settingsView;

    private boolean isOpen;

    View mainView;

    static final int STATE_PLAYER = 0;
    static final int STATE_OPPONENT = 1;
    static final int STATE_SETTINGS = 2;
    private int state;

    public static DeckCompanion getPlayerCompanion() {
        MainViewCompanion.get();
        return sPlayerCompanion;
    }

    public static DeckCompanion getOpponentCompanion() {
        MainViewCompanion.get();
        return sOpponentCompanion;
    }

    public void setState(int newState) {
        state = newState;
        updateState();
    }

    private void updateState() {
        playerView.setVisibility(GONE);
        opponentView.setVisibility(GONE);
        settingsView.setVisibility(GONE);

        if (isOpen) {
            switch (state) {
                case STATE_PLAYER:
                    playerView.setVisibility(View.VISIBLE);
                    break;
                case STATE_OPPONENT:
                    opponentView.setVisibility(View.VISIBLE);
                    break;
                case STATE_SETTINGS:
                    settingsView.setVisibility(View.VISIBLE);
                    break;
            }
        }

        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        mainView.measure(wMeasureSpec, wMeasureSpec);

        mParams.w = mainView.getMeasuredWidth();

        mViewManager.updateView(mainView, mParams);
    }

    private static MainViewCompanion sMainCompanion;

    public static MainViewCompanion get() {
        if (sMainCompanion == null) {
            View view = LayoutInflater.from(ArcaneTrackerApplication.getContext()).inflate(R.layout.main_view, null);
            sMainCompanion = new MainViewCompanion(view);
        }

        return sMainCompanion;
    }

    public MainViewCompanion(View v) {
        mainView = v;
        mViewManager = ViewManager.get();

        mParams = new ViewManager.Params();
        mParams.x = 0;
        mParams.y = 0;
        mParams.w = 0;
        mParams.h = mViewManager.getHeight();

        playerView = v.findViewById(R.id.playerView);
        opponentView = v.findViewById(R.id.opponentView);
        settingsView = v.findViewById(R.id.settingsView);

        int width = (int) (0.33 * 0.5 * mViewManager.getWidth());

        ViewGroup.LayoutParams params;
        params = playerView.getLayoutParams();
        params.width = width;
        playerView.setLayoutParams(params);
        sPlayerCompanion = new DeckCompanion(playerView, false);

        params = opponentView.getLayoutParams();
        params.width = width;
        opponentView.setLayoutParams(params);
        sOpponentCompanion = new DeckCompanion(opponentView, false);

        width = (int) (0.4 * mViewManager.getWidth());
        params = settingsView.getLayoutParams();
        params.width = width;
        settingsView.setLayoutParams(params);

        new SettingsCompanion(this, settingsView);

        configureHandles(v);

        setAlphaSetting(getAlphaSetting());

        setState(STATE_PLAYER);
    }

    private void configureHandles(View v) {
        HandleView handleView = (HandleView) v.findViewById(R.id.settingsHandle);
        Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.settings_handle);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.gray));
        handleView.setOnClickListener(v2 -> {
            setOpen(!isOpen);
            setState(STATE_SETTINGS);
        });

        handleView = (HandleView) v.findViewById(R.id.opponentHandle);
        drawable = v.getContext().getResources().getDrawable(R.drawable.icon_white);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.opponentColor));
        handleView.setOnClickListener(v2 -> {
            setOpen(!isOpen);
            setState(STATE_OPPONENT);
        });

        handleView = (HandleView) v.findViewById(R.id.playerHandle);
        drawable = v.getContext().getResources().getDrawable(R.drawable.icon_white);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.colorPrimary));
        handleView.setOnClickListener(v2 -> {
            setOpen(!isOpen);
            setState(STATE_PLAYER);
        });
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
        updateState();
    }

    public void show(boolean show) {
        if (show) {
            mViewManager.addView(mainView, mParams);
        } else {
            mViewManager.removeView(mainView);
        }
    }

    public void setAlphaSetting(int progress) {
        mainView.setAlpha(0.5f + progress/200f);
        Settings.set(Settings.ALPHA, progress);
    }

    public int getAlphaSetting() {
        return Settings.get(Settings.ALPHA, 100);
    }
}
