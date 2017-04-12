package net.mbonnin.arcanetracker;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 10/25/16.
 */

public class SettingsButtonCompanion {

    private final MainViewCompanion mainViewCompanion;
    private final DeckListManager deckListManager;
    private ViewManager mViewManager = null;

    public SettingsButtonCompanion(View settingsButton, MainViewCompanion mainViewCompanion, DeckListManager deckListManager, ViewManager viewManager) {
       this.mainViewCompanion = mainViewCompanion;
       this.deckListManager = deckListManager;
       this.mViewManager = viewManager;

        settingsButton.setOnClickListener(gettingClickListener());
    }

    private View.OnClickListener gettingClickListener() {
    return v -> {
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.menu_view, null);

            int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(wMeasureSpec, wMeasureSpec);

            int a[] = new int[2];
            v.getLocationOnScreen(a);

            view.findViewById(R.id.changeDeck).setOnClickListener(v2 -> {
                mViewManager.removeView(view);

                View deckListView = LayoutInflater.from(v2.getContext()).inflate(R.layout.decklist_view, null);
                RecyclerView recyclerView = (RecyclerView) deckListView.findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(v2.getContext()));
                DeckListAdapter adapter = new DeckListAdapter(deckListManager);
                adapter.setOnDeckSelectedListener(deck -> {
                    mViewManager.removeView(deckListView);
                    mainViewCompanion.getPlayerCompanion().setDeck(deck);
                });
                recyclerView.setAdapter(adapter);

                deckListView.measure(wMeasureSpec, wMeasureSpec);

                int h = deckListView.getMeasuredHeight();
                if (h > mViewManager.getHeight()) {
                    h = mViewManager.getHeight();
                }
                ViewManager.Params params = new ViewManager.Params();
                params.x = a[0] + v.getWidth() - recyclerView.getMeasuredWidth();
                params.y = a[1] + v.getHeight() / 2 - h;
                params.w = deckListView.getMeasuredWidth();
                params.h = h;

                mViewManager.addModalView(deckListView, params);
            });

            view.findViewById(R.id.editDeck).setOnClickListener(v2 -> {
                mViewManager.removeView(view);

                DeckEditorView.show(v.getContext(), mainViewCompanion.getPlayerCompanion().getDeck());
            });

            view.findViewById(R.id.createDeck).setOnClickListener(v2 -> {
                mViewManager.removeView(view);

                View view2 = CreateDeckView.create(v2.getContext());
                view2.measure(wMeasureSpec, wMeasureSpec);

                ViewManager.Params params = new ViewManager.Params();
                params.x = mViewManager.getWidth() / 4;
                params.y = mViewManager.getHeight() / 16;
                params.w = mViewManager.getWidth() / 2;
                params.h = 7 * mViewManager.getHeight() / 8;

                mViewManager.addModalAndFocusableView(view2, params);
            });

            view.findViewById(R.id.deleteDeck).setOnClickListener(v2 -> {
                mViewManager.removeView(view);
                View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.delete_confirmation_view, null);
                view2.findViewById(R.id.deleteButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                    deckListManager.deleteDeck(mainViewCompanion.getPlayerCompanion().getDeck());
                    List<Deck> list = deckListManager.get();
                    Deck newDeck;

                    if (!list.isEmpty()) {
                        newDeck = list.get(0);
                    } else {
                        newDeck = deckListManager.createDeck(Card.CLASS_INDEX_WARRIOR);
                    }
                    mainViewCompanion.getPlayerCompanion().setDeck(newDeck);
                });
                view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                });
                mViewManager.addCenteredView(view2);
            });

            view.findViewById(R.id.renameDeck).setOnClickListener(v2 -> {
                mViewManager.removeView(view);

                Deck deck = mainViewCompanion.getPlayerCompanion().getDeck();
                if (deck.id.equals(deckListManager.ARENA_DECK_ID)) {
                    Toast.makeText(view.getContext(), "Sorry, you cannot rename the Arena deck !", Toast.LENGTH_LONG).show();
                    return;
                }
                View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.rename_deck_view, null);

                ((EditText)(view2.findViewById(R.id.editText))).setText(deck.name);
                view2.findViewById(R.id.renameButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                    deck.name = ((EditText)(view2.findViewById(R.id.editText))).getText().toString();
                    mainViewCompanion.getPlayerCompanion().setDeck(deck);
                    deckListManager.save();

                });
                view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                });
                mViewManager.addCenteredView(view2);
            });

            view.findViewById(R.id.resetCounter).setOnClickListener(v2 -> {
                mViewManager.removeView(view);


                View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.reset_counter_confirm_view, null);
                view2.findViewById(R.id.resetButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                    Deck deck = mainViewCompanion.getPlayerCompanion().getDeck();
                    deck.wins = 0;
                    deck.losses = 0;
                    mainViewCompanion.getPlayerCompanion().setDeck(deck);
                });
                view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                    mViewManager.removeView(view2);
                });

                mViewManager.addCenteredView(view2);
            });

            ViewManager.Params params = new ViewManager.Params();
            params.x = a[0] + v.getWidth() - view.getMeasuredWidth();
            params.y = a[1] + v.getHeight() / 2 - view.getMeasuredHeight();
            params.w = view.getMeasuredWidth();
            params.h = view.getMeasuredHeight();

            mViewManager.addModalView(view, params);
        };
    }

}
