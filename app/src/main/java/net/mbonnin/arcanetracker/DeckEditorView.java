package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.mbonnin.arcanetracker.adapter.EditableItemAdapter;
import net.mbonnin.hsmodel.PlayerClass;

import java.util.ArrayList;
import java.util.Locale;

public class DeckEditorView extends RelativeLayout {

    RecyclerView cardsRecyclerView;
    RecyclerView deckRecyclerView;
    ManaSelectionView manaSelectionView;
    EditText editText;
    TextView cardCount;
    Button button;
    View classImageView;
    View neutralImageView;
    private Deck mDeck;
    private CardsAdapter mCardsAdapter;
    private EditableItemAdapter mDeckAdapter;
    private CardsAdapter.Listener mCardsAdapterListener = card -> {
        mDeckAdapter.addCard(card.id);
        updateCardCount();
    };

    private ImageButton close;

    private RecyclerView.AdapterDataObserver mDeckAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();

            ArrayList<String> list = mDeckAdapter.getDisabledCards();
            mCardsAdapter.setDisabledCards(list);
            updateCardCount();
        }
    };

    private View classImageViewDisabled;
    private View neutralImageViewDisabled;
    private String mClass;

    private void updateCardCount() {
        cardCount.setText(mDeck.getCardCount() + "/ 30");
    }

    public DeckEditorView(Context context) {
        super(context);
    }

    public DeckEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static DeckEditorView build(Context context) {
        return (DeckEditorView) LayoutInflater.from(new ContextThemeWrapper(context, R.style.AppTheme)).inflate(R.layout.deck_editor_view, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        cardsRecyclerView = findViewById(R.id.cardsRecyclerView);
        deckRecyclerView = findViewById(R.id.deckRecyclerView);
        manaSelectionView = findViewById(R.id.manaSelectionView);
        editText = findViewById(R.id.editText);
        cardCount = findViewById(R.id.cardCount);
        button = findViewById(R.id.button);
        close = findViewById(R.id.close);
        classImageView = findViewById(R.id.classImageView);
        neutralImageView = findViewById(R.id.neutralImageView);
        classImageViewDisabled = findViewById(R.id.classImageViewDisabled);
        neutralImageViewDisabled = findViewById(R.id.neutralImageViewDisabled);

        findViewById(R.id.filters).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    public void setDeck(Deck deck) {
        mDeck = deck;

        String names[] = new String[2];
        String playerClass = HeroUtilKt.getPlayerClass(mDeck.classIndex);
        names[0] = playerClass.substring(0, 1) + playerClass.substring(1).toLowerCase();
        names[1] = "Neutral";

        mCardsAdapter = new CardsAdapter();
        cardsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        cardsRecyclerView.setAdapter(mCardsAdapter);

        mDeckAdapter = new EditableItemAdapter();
        mDeckAdapter.setDeck(deck);
        mDeckAdapter.registerAdapterDataObserver(mDeckAdapterObserver);
        ArrayList<String> list = mDeckAdapter.getDisabledCards();
        mCardsAdapter.setDisabledCards(list);

        deckRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        deckRecyclerView.setAdapter(mDeckAdapter);


        mClass = playerClass;

        mCardsAdapter.setListener(mCardsAdapterListener);
        mCardsAdapter.setClass(mClass);

        classImageView.setBackgroundDrawable(Utils.INSTANCE.getDrawableForName(String.format(Locale.ENGLISH, "hero_%02d_round", mDeck.classIndex + 1)));
        classImageViewDisabled.setVisibility(GONE);
        neutralImageView.setBackgroundResource(R.drawable.hero_10_round);


        classImageView.setOnClickListener(v -> {
            mCardsAdapter.setClass(mClass);
            cardsRecyclerView.scrollToPosition(0);
            classImageViewDisabled.setVisibility(GONE);
            neutralImageViewDisabled.setVisibility(VISIBLE);
        });

        neutralImageView.setOnClickListener(v -> {
            mCardsAdapter.setClass(PlayerClass.NEUTRAL);
            cardsRecyclerView.scrollToPosition(0);
            classImageViewDisabled.setVisibility(VISIBLE);
            neutralImageViewDisabled.setVisibility(GONE);
        });

        manaSelectionView.setListener(index -> {
            mCardsAdapter.setCost(index);
            cardsRecyclerView.scrollToPosition(0);
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mCardsAdapter.setSearchQuery(editText.getText().toString());
                cardsRecyclerView.scrollToPosition(0);
            }
        });

        editText.setOnEditorActionListener((v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
            }
            return false;
        });

        updateCardCount();

        close.setOnClickListener(v -> editText.setText(""));

        button.setOnClickListener(v -> {
            DeckList.INSTANCE.save();
            DeckList.INSTANCE.saveArena();
            //DeckList.getPlayerGameDeck().clear();
            MainViewCompanion.Companion.getLegacyCompanion().setDeck(deck);
            ViewManager.Companion.get().removeView(this);
        });
    }

    public static DeckEditorView show(Deck deck) {
        Context context = ArcaneTrackerApplication.Companion.getContext();
        DeckEditorView deckEditorView = DeckEditorView.build(context);

        ViewManager viewManager = ViewManager.Companion.get();
        ViewManager.Params params = new ViewManager.Params();
        params.setX(0);
        params.setY(0);
        params.setW(ViewManager.Companion.get().getUsableWidth());
        params.setH(ViewManager.Companion.get().getUsableHeight());

        deckEditorView.setDeck(deck);
        viewManager.addModalAndFocusableView(deckEditorView, params);

        return deckEditorView;
    }
}
