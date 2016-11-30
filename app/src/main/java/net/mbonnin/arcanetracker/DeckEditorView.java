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

import java.util.ArrayList;

/**
 * Created by martin on 10/21/16.
 */

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

        cardsRecyclerView = (RecyclerView) findViewById(R.id.cardsRecyclerView);
        deckRecyclerView = (RecyclerView) findViewById(R.id.deckRecyclerView);
        manaSelectionView = (ManaSelectionView) findViewById(R.id.manaSelectionView);
        editText = (EditText) findViewById(R.id.editText);
        cardCount = (TextView)findViewById(R.id.cardCount);
        button = (Button)findViewById(R.id.button);
        close = (ImageButton)findViewById(R.id.close);
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
        names[0] = Card.classNameList[mDeck.classIndex];
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


        mClass = Card.classIndexToPlayerClass(mDeck.classIndex);

        mCardsAdapter.setListener(mCardsAdapterListener);
        mCardsAdapter.setClass(mClass);

        classImageView.setBackgroundDrawable(Utils.getDrawableForName(String.format("hero_%02d_round", mDeck.classIndex + 1)));
        classImageViewDisabled.setVisibility(GONE);
        neutralImageView.setBackgroundResource(R.drawable.hero_10_round);


        classImageView.setOnClickListener(v -> {
            mCardsAdapter.setClass(mClass);
            cardsRecyclerView.scrollToPosition(0);
            classImageViewDisabled.setVisibility(GONE);
            neutralImageViewDisabled.setVisibility(VISIBLE);
        });

        neutralImageView.setOnClickListener(v -> {
            mCardsAdapter.setClass(Card.CLASS_NEUTRAL);
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
            DeckList.save();
            DeckList.saveArena();
            //DeckList.getPlayerGameDeck().clear();
            MainViewCompanion.getPlayerCompanion().setDeck(deck);
            ViewManager.get().removeView(this);
        });
    }

    public static DeckEditorView show(Deck deck) {
        Context context = ArcaneTrackerApplication.getContext();
        DeckEditorView deckEditorView = DeckEditorView.build(context);

        ViewManager viewManager = ViewManager.get();
        ViewManager.Params params = new ViewManager.Params();
        params.x = 0;
        params.y = 0;
        params.w = (int) (viewManager.getUsableWidth());
        params.h = viewManager.getUsableHeight();

        deckEditorView.setDeck(deck);
        viewManager.addModalAndFocusableView(deckEditorView, params);

        return deckEditorView;
    }
}
