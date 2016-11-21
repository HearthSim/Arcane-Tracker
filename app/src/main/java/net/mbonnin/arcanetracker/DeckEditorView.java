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
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.mbonnin.arcanetracker.adapter.EditableItemAdapter;

import java.util.ArrayList;

/**
 * Created by martin on 10/21/16.
 */

public class DeckEditorView extends LinearLayout {

    RecyclerView cardsRecyclerView;
    RecyclerView deckRecyclerView;
    ManaSelectionView manaSelectionView;
    Spinner spinner;
    EditText editText;
    TextView cardCount;
    Button button;
    private Deck mDeck;
    private CardsAdapter mCardsAdapter;
    private EditableItemAdapter mDeckAdapter;
    private CardsAdapter.Listener mCardsAdapterListener = card -> {
        mDeck.addCard(card.id, 1);
        updateCardCount();
    };

    private ImageButton close;

    private RecyclerView.AdapterDataObserver mAdapterObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();

            ArrayList<String> list = mDeckAdapter.getDisabledCards();
            mCardsAdapter.setDisabledCards(list);
            updateCardCount();
        }
    };

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
        return (DeckEditorView) LayoutInflater.from(new ContextThemeWrapper(context, android.R.style.Theme_Material_Light)).inflate(R.layout.deck_editor_view, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        cardsRecyclerView = (RecyclerView) findViewById(R.id.cardsRecyclerView);
        deckRecyclerView = (RecyclerView) findViewById(R.id.deckRecyclerView);
        manaSelectionView = (ManaSelectionView) findViewById(R.id.manaSelectionView);
        spinner = (Spinner) findViewById(R.id.spinner);
        editText = (EditText) findViewById(R.id.editText);
        cardCount = (TextView)findViewById(R.id.cardCount);
        button = (Button)findViewById(R.id.button);
        close = (ImageButton)findViewById(R.id.close);
    }

    public void setDeck(Deck deck) {
        mDeck = deck;

        String names[] = new String[2];
        names[0] = Card.classNameList[mDeck.classIndex];
        names[1] = "Neutral";

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mCardsAdapter.setClass(mDeck.classIndex);
                } else {
                    mCardsAdapter.setClass(Card.CLASS_INDEX_NEUTRAL);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCardsAdapter = new CardsAdapter();
        mCardsAdapter.setClass(mDeck.classIndex);
        cardsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        cardsRecyclerView.setAdapter(mCardsAdapter);

        mDeckAdapter = new EditableItemAdapter();
        mDeckAdapter.setDeck(deck);
        mDeckAdapter.registerAdapterDataObserver(mAdapterObserver);
        ArrayList<String> list = mDeckAdapter.getDisabledCards();
        mCardsAdapter.setDisabledCards(list);

        deckRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        deckRecyclerView.setAdapter(mDeckAdapter);

        mCardsAdapter.setListener(mCardsAdapterListener);

        manaSelectionView.setListener(index -> {
            mCardsAdapter.setCost(index);
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
        params.w = viewManager.getUsableWidth();
        params.h = viewManager.getUsableHeight();

        deckEditorView.setDeck(deck);
        viewManager.addModalAndFocusableView(deckEditorView, params);

        return deckEditorView;
    }
}
