package net.mbonnin.arcanetracker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.paperdb.Paper;
import timber.log.Timber;

/**
 * Created by martin on 10/14/16.
 */

public class ArcaneView extends LinearLayout implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private static final String KEY_LAST_USED_DECK_ID = "KEY_LAST_USED_DECK_ID";
    private static ArcaneView sPlayerArcaneView;
    private static ArcaneView sOpponentArcaneView;

    View settings;
    ImageButton expand;
    public TextView deckName;
    HandleView handleView;

    private DeckAdapter mAdapter;
    private RecyclerView recyclerView;
    private ViewManager mViewManager;

    private ViewManager.Params mParams;
    private ViewManager.Params mRecyclerViewParams;
    private boolean isOpponent;
    private Deck mDeck;
    private ImageView background;
    private int mAlphaSetting;
    private float mCx, mCy, mRadius;
    private float mSmallRadius;
    private float mBigRadius;
    private ValueAnimator mValueAnimator;
    private boolean mMinimized;
    private int mColor;

    public ArcaneView(Context context) {
        super(context);
    }

    public ArcaneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(boolean isOpponent) {
        mViewManager = ViewManager.get();
        this.isOpponent = isOpponent;

        setOrientation(VERTICAL);
        Timber.d("screen: " + mViewManager.getWidth() + "x" + mViewManager.getHeight());

        int w = (int) (0.33 * 0.5 * mViewManager.getWidth());
        int h = mViewManager.getHeight();

        settings = findViewById(R.id.settings);
        expand = (ImageButton) findViewById(R.id.expand);
        deckName = (TextView) findViewById(R.id.deckName);
        background = (ImageView) findViewById(R.id.background);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        expand.setOnClickListener(v -> minimize(true, true));

        int x = Settings.get("x" + isOpponent, -1);
        if (x == -1) {
            x = 0;
        }
        mParams = new ViewManager.Params();
        mParams.x = x;
        mParams.y = 0;
        mParams.w = w;
        mParams.h = h;

        if (isOpponent) {
            mColor = getContext().getResources().getColor(R.color.opponentColor);
        } else {
            mColor = getContext().getResources().getColor(R.color.colorPrimary);
        }


        mRecyclerViewParams = new ViewManager.Params();
        mRecyclerViewParams.w = w;
        mRecyclerViewParams.h = mViewManager.getHeight() - h;

        if (isOpponent) {
            settings.setVisibility(GONE);
            mAdapter = new DeckAdapter();
            setDeck(DeckList.getOpponentGameDeck());
        } else {
            new SettingsButtonHolder(settings);
            String lastUsedId = Paper.book().read(KEY_LAST_USED_DECK_ID);

            Deck deck = null;
            if (lastUsedId != null) {
                for (Deck deck2 : DeckList.get()) {
                    if (deck2.id.equals(lastUsedId)) {
                        deck = deck2;
                        break;
                    }
                }
                if (deck == null && lastUsedId.equals(DeckList.ARENA_DECK_ID)) {
                    deck = DeckList.getArenaDeck();
                }
            }

            if (deck == null) {
                deck = DeckList.createDeck(Card.CLASS_INDEX_WARRIOR);
                Paper.book().write(KEY_LAST_USED_DECK_ID, deck.id);
            }

            mAdapter = new PlayerDeckAdapter();
            setDeck(deck);
        }

        recyclerView.setBackgroundColor(Color.BLACK);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);

        mAlphaSetting = Settings.get(Settings.ALPHA + isOpponent, 100);
        setAlphaSetting(mAlphaSetting);

        int size = Utils.dpToPx(50);
        mCx = size/2 + Utils.dpToPx(10);
        mCy = (float) (mViewManager.getHeight() * 0.8);
        if (isOpponent) {
            mCy -= size + Utils.dpToPx(10);
        }
        mBigRadius = (float) Math.hypot(mCx, mCy);
        mSmallRadius = size/2;

        mRadius = 0;
        handleView = new HandleView(getContext());
        handleView.setSize(size);
        handleView.setImageDrawable(isOpponent ? getResources().getDrawable(R.drawable.ic_hearthstone_icon_round_red):getResources().getDrawable(R.drawable.ic_hearthstone_icon_round));
        handleView.setOnClickListener(v -> minimize(false, true));
        handleView.setOrigin((int) mCx - size / 2, (int) (mCy - size / 2));

        mValueAnimator = new ValueAnimator();
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.addListener(this);
        mValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void show(boolean show) {
        if (show) {
            setVisibility(VISIBLE);
            mViewManager.addView(this, mParams);
        } else {
            setVisibility(GONE);
            mViewManager.removeView(this);
        }
    }

    public static View build(Context context) {
        ArcaneView view = (ArcaneView) LayoutInflater.from(context).inflate(R.layout.arcane_view, null);
        return view;

    }

    public void setDeck(Deck deck) {
        if (!isOpponent) {
            Paper.book().write(KEY_LAST_USED_DECK_ID, deck.id);
        }

        deck.checkClassIndex();

        mDeck = deck;
        background.setBackgroundDrawable(Utils.getDrawableForClassIndex(deck.classIndex));
        deckName.setText(deck.name);
        mAdapter.setDeck(deck);
    }

    public static ArcaneView getPlayerAnchorView() {
        if (sPlayerArcaneView == null) {
            sPlayerArcaneView = (ArcaneView) ArcaneView.build(ArcaneTrackerApplication.getContext());
            sPlayerArcaneView.init(false);
        }
        return sPlayerArcaneView;
    }

    public Deck getDeck() {
        return mDeck;
    }

    public int setAlphaSetting(int alpha) {
        Settings.set(Settings.ALPHA + isOpponent, alpha);
        setAlpha(0.5f + alpha / 200f);
        return mAlphaSetting;
    }

    public int getAlphaSetting() {
        return mAlphaSetting;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mRadius > 0) {
            Path path = new Path();
            path.addCircle(mCx, mCy, mRadius, Path.Direction.CCW);
            canvas.clipPath(path);
        }
        super.dispatchDraw(canvas);
    }

    public void minimize(boolean minimize, boolean animate) {
        float initialRadius;
        float finalRadius;

        if (minimize) {
            initialRadius = mBigRadius;
            finalRadius = mSmallRadius;
        } else {
            initialRadius = mSmallRadius;
            finalRadius = mBigRadius;
        }

        mMinimized = minimize;
        if (animate) {
            show(true);
            mValueAnimator.cancel();
            mValueAnimator.setFloatValues(initialRadius, finalRadius);
            mValueAnimator.start();
        } else {
            show(!minimize);
            handleView.show(minimize);
        }

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mRadius = (float) animation.getAnimatedValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            float a = 1.0f - (mRadius - mSmallRadius) / (mBigRadius - mSmallRadius);
            int c2 = Color.argb((int) (a*255), Color.red(mColor), Color.green(mColor), Color.blue(mColor));
            setForeground(new ColorDrawable(c2));
        }
        invalidate();
    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        handleView.show(mMinimized);
        show(!mMinimized);
        mRadius = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForeground(null);
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    public static ArcaneView getOpponentAnchorView() {
        if (sOpponentArcaneView == null) {
            sOpponentArcaneView = (ArcaneView) ArcaneView.build(ArcaneTrackerApplication.getContext());
            sOpponentArcaneView.init(true);
        }
        return sOpponentArcaneView;
    }
}

