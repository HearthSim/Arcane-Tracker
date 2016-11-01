package net.mbonnin.arcanetracker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.view.menu.MenuWrapperFactory;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import timber.log.Timber;

import static android.view.View.GONE;

public class MainViewCompanion implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private static DeckCompanion sOpponentCompanion;
    private final Handler mHandler;
    private ViewManager.Params mParams;
    private static DeckCompanion sPlayerCompanion;
    private final View shadow;
    private int mWidth = 0;
    private final View frameLayout;
    private final int mPadding;

    private float mRefY;
    private float mRefX;
    private int mTouchSlop;
    private float mDownY;
    private float mDownX;

    private ViewManager mViewManager;

    private View playerView;
    private View opponentView;

    private HandlesView handlesView;
    private ValueAnimator mAnimator;
    private boolean isOpen;

    View mainView;

    static final int STATE_PLAYER = 0;
    static final int STATE_OPPONENT = 1;
    private int state;
    private Integer mX;

    private int mHandlesMovement;
    private static final int HANDLES_MOVEMENT_X = 1;
    private static final int HANDLES_MOVEMENT_Y = 2;

    private int direction = 1;
    private float velocityRefX;
    private float velocityLastX;
    private long velocityRefTime;


    private final View.OnTouchListener mHandlesViewTouchListener = (v, ev) -> {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getRawX();
            mDownY = ev.getRawY();
            mRefX = handlesView.getParams().x;
            mRefY = handlesView.getParams().y;
            mHandlesMovement = 0;

        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mHandlesMovement == 0) {
                if (Math.abs(ev.getRawX() - mDownX) > mTouchSlop) {
                    prepareAnimation();

                    velocityRefX = ev.getRawX();
                    velocityLastX = ev.getRawX();
                    velocityRefTime = System.nanoTime();

                    mHandlesMovement = HANDLES_MOVEMENT_X;
                } else if (Math.abs(ev.getRawY() - mDownY) > mTouchSlop) {
                    mHandlesMovement = HANDLES_MOVEMENT_Y;
                }
            }

            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                if ((ev.getRawX() - velocityLastX) * direction > 0) {
                    velocityLastX = ev.getRawX();
                } else {
                    direction = -direction;
                    velocityRefX = ev.getRawX();
                    velocityLastX = ev.getRawX();
                    velocityRefTime = System.nanoTime();
                }
                int newX = (int) (mRefX + ev.getRawX() - mDownX);
                if (newX > mWidth) {
                    newX = mWidth;
                } else if (newX < 0) {
                    newX = 0;
                }
                setX(newX);
            } else if (mHandlesMovement == HANDLES_MOVEMENT_Y) {
                handlesView.getParams().y = (int) (mRefY + ev.getRawY() - mDownY);
                handlesView.update();
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL || ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                float velocity = 0;
                long timeDiff = System.nanoTime() - velocityRefTime;
                if (timeDiff > 0) {
                    velocity = 1000 * 1000 * (ev.getRawX() - velocityRefX) / timeDiff;
                }
                Timber.w("velocity: %f", velocity);
                if (mX < mWidth) {
                    if (velocity <= 0) {
                        animateXTo(0, velocity);
                    } else if (velocity > 0) {
                        animateXTo(mWidth, velocity);
                    }
                }
            }
        }

        return mHandlesMovement != 0;
    };

    private Runnable mHideViewRunnable = () -> {
        mParams.w = 0;
        mViewManager.updateView(mainView, mParams);
    };

    private void prepareAnimation() {
        mHandler.removeCallbacks(mHideViewRunnable);
        mParams.w = mWidth;
        mViewManager.updateView(mainView, mParams);
    }
    private void animateXTo(int targetX, float pixelPerMillisecond) {
        pixelPerMillisecond = Math.abs(pixelPerMillisecond);
        if (pixelPerMillisecond < 0.6) {
            pixelPerMillisecond = 0.6f;
        }
        mAnimator.cancel();

        mAnimator.setInterpolator(new LinearInterpolator());
        if (pixelPerMillisecond > 0) {
            mAnimator.setDuration((long) (Math.abs(mX - targetX) / pixelPerMillisecond));
        } else {
            mAnimator.setDuration(300);
        }

        prepareAnimation();
        mAnimator.setIntValues(mX, targetX);
        mAnimator.start();
    }

    private void animateXTo(int targetX) {
        mAnimator.cancel();
        mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimator.setDuration(300);

        prepareAnimation();
        mAnimator.setIntValues(mX, targetX);
        mAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        setX((Integer) animation.getAnimatedValue());
    }

    private void setX(int x) {
        //Timber.w("setX: %d", mX);
        mX = x;
        mainView.setTranslationX(-mWidth + mX);
        handlesView.getParams().x = mX + mPadding;
        handlesView.update();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        //Timber.w("onAnimationEnd: %d", mX);
        if (mX == 0) {
            /**
             * XXX: somehow if I do this too early, there a small glitch on screen...
             */
            mHandler.postDelayed(mHideViewRunnable, 300);
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    class ClickListener implements View.OnClickListener {
        private final int newState;

        public ClickListener(int newState) {
            this.newState = newState;
        }

        @Override
        public void onClick(View v) {
            if (state == newState && isOpen) {
                setState(state, false);
            } else {
                setState(newState, true);
            }
        }
    }

    public static DeckCompanion getPlayerCompanion() {
        MainViewCompanion.get();
        return sPlayerCompanion;
    }

    public static DeckCompanion getOpponentCompanion() {
        MainViewCompanion.get();
        return sOpponentCompanion;
    }

    public void setState(int newState, boolean newOpen) {
        if (newOpen) {
            switch (newState) {
                case STATE_PLAYER:
                    opponentView.setVisibility(View.GONE);
                    playerView.setVisibility(View.VISIBLE);
                    break;
                case STATE_OPPONENT:
                    opponentView.setVisibility(View.VISIBLE);
                    playerView.setVisibility(View.GONE);
                    break;
            }
        }

        if (newOpen != isOpen) {
            animateXTo(newOpen ? mWidth : 0);
        }

        state = newState;
        this.isOpen = newOpen;
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

        mHandler = new Handler();

        ViewConfiguration vc = ViewConfiguration.get(v.getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        mAnimator = new ValueAnimator();
        mAnimator.addUpdateListener(this);
        mAnimator.addListener(this);
        frameLayout = v.findViewById(R.id.frameLayout);
        playerView = v.findViewById(R.id.playerView);
        opponentView = v.findViewById(R.id.opponentView);
        shadow = v.findViewById(R.id.shadow);

        mWidth = (int) (0.33 * 0.5 * mViewManager.getWidth());
        mX = 0;
        mPadding = Utils.dpToPx(5);

        mParams = new ViewManager.Params();
        mParams.x = 0;
        mParams.y = 0;
        mParams.w = 0;
        mParams.h = mViewManager.getHeight();

        sPlayerCompanion = new DeckCompanion(playerView, false);
        sOpponentCompanion = new DeckCompanion(opponentView, true);

        handlesView = (HandlesView) LayoutInflater.from(v.getContext()).inflate(R.layout.handles_view, null);
        handlesView.setListener(mHandlesViewTouchListener);
        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        handlesView.measure(wMeasureSpec, wMeasureSpec);
        handlesView.getParams().w = handlesView.getMeasuredWidth();
        handlesView.getParams().h = handlesView.getMeasuredHeight();
        handlesView.getParams().x = mPadding;
        handlesView.getParams().y = ViewManager.get().getHeight() - handlesView.getParams().h - Utils.dpToPx(50);
        configureHandles(handlesView);

        setState(STATE_PLAYER, false);

        setAlphaSetting(getAlphaSetting());
    }

    public void show() {
        mViewManager.addView(mainView, mParams);
        handlesView.show();
    }

    private void configureHandles(View v) {
        HandleView handleView = (HandleView) v.findViewById(R.id.settingsHandle);
        Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.settings_handle);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.gray));
        handleView.setOnClickListener(v2 -> SettingsCompanion.show());

        handleView = (HandleView) v.findViewById(R.id.opponentHandle);
        drawable = v.getContext().getResources().getDrawable(R.drawable.icon_white);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.opponentColor));
        handleView.setOnClickListener(new ClickListener(STATE_OPPONENT));

        handleView = (HandleView) v.findViewById(R.id.playerHandle);
        drawable = v.getContext().getResources().getDrawable(R.drawable.icon_white);
        handleView.init(drawable, v.getContext().getResources().getColor(R.color.colorPrimary));
        handleView.setOnClickListener(new ClickListener(STATE_PLAYER));
    }


    public void setAlphaSetting(int progress) {
        float a = 0.5f + progress / 200f;
        mainView.setAlpha(a);
        handlesView.setAlpha(a);
        Settings.set(Settings.ALPHA, progress);
    }

    public int getAlphaSetting() {
        return Settings.get(Settings.ALPHA, 100);
    }
}
