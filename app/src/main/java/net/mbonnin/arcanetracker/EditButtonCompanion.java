package net.mbonnin.arcanetracker;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.mbonnin.hsmodel.Card;
import net.mbonnin.arcanetracker.databinding.ImportDeckstringBinding;

import java.util.ArrayList;

/**
 * Created by martin on 10/25/16.
 */

public class EditButtonCompanion {

    private ViewManager mViewManager = null;

    private View.OnClickListener mOnEditClickListener = v -> {
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
            DeckListAdapter adapter = new DeckListAdapter();
            adapter.setOnDeckSelectedListener(deck -> {
                mViewManager.removeView(deckListView);
                MainViewCompanion.getPlayerCompanion().setDeck(deck);
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

            DeckEditorView.show(MainViewCompanion.getPlayerCompanion().getDeck());
        });

        view.findViewById(R.id.deleteDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);
            View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.delete_confirmation_view, null);
            view2.findViewById(R.id.deleteButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
                DeckList.deleteDeck(MainViewCompanion.getPlayerCompanion().getDeck());
                ArrayList<Deck> list = DeckList.get();
                Deck newDeck;

                if (!list.isEmpty()) {
                    newDeck = list.get(0);
                } else {
                    newDeck = DeckList.createDeck(Card.CLASS_INDEX_WARRIOR);
                }
                MainViewCompanion.getPlayerCompanion().setDeck(newDeck);
            });
            view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
            });
            mViewManager.addCenteredView(view2);
        });

        view.findViewById(R.id.renameDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);

            Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();
            if (deck.isArena()) {
                Toast.makeText(view.getContext(), "Sorry, you cannot rename the Arena deck !", Toast.LENGTH_LONG).show();
                return;
            }
            View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.rename_deck_view, null);

            ((EditText) (view2.findViewById(R.id.editText))).setText(deck.name);
            view2.findViewById(R.id.renameButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
                deck.name = ((EditText) (view2.findViewById(R.id.editText))).getText().toString();
                MainViewCompanion.getPlayerCompanion().setDeck(deck);
                DeckList.save();

            });
            view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
            });
            mViewManager.addCenteredView(view2);
        });

        view.findViewById(R.id.createDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);

            newDeckClicked(view.getContext());
        });

        ViewManager.Params params = new ViewManager.Params();
        params.x = a[0];
        params.y = a[1] + v.getHeight() / 2 - view.getMeasuredHeight();
        params.w = view.getMeasuredWidth();
        params.h = view.getMeasuredHeight();

        mViewManager.addModalView(view, params);
    };

    private void newDeckClicked(Context context) {
        ImportDeckstringBinding binding = ImportDeckstringBinding.inflate(LayoutInflater.from(context));
        View view = binding.getRoot();

        String pasteData = getPasteData(context);
        if (pasteData != null && DeckString.parse(pasteData) != null) {
            binding.editText.setText(pasteData);
        }

        binding.useDeckString.setOnClickListener(v -> {
            Deck deck = DeckString.parse(binding.editText.getText().toString());
            if (deck != null) {
                mViewManager.removeView(view);
                DeckList.addDeck(deck);
                MainViewCompanion.getPlayerCompanion().setDeck(deck);
            } else {
                Toast.makeText(context, Utils.getString(R.string.cannotParseDeckstring), Toast.LENGTH_LONG).show();
            }
        });

        binding.createNewDeck.setOnClickListener(v -> {
            mViewManager.removeView(view);
            showNewDeckDialog(context);
        });

        ViewManager.Params params = new ViewManager.Params();
        ViewManager viewManager = ViewManager.get();

        params.x = viewManager.getWidth() / 8;
        params.y = viewManager.getHeight() / 16;
        params.w = 3 * viewManager.getWidth() / 4;
        params.h = 7 * viewManager.getHeight() / 8;

        mViewManager.addModalAndFocusableView(view, params);
    }

    private String getPasteData(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (!clipboard.hasPrimaryClip()) {
            return null;
        }

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

        if (item.getText() == null) {
            return null;

        }
        return item.getText().toString();
    }

    private void showNewDeckDialog(Context context) {
        View view2 = CreateDeckView.create(context);
        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view2.measure(wMeasureSpec, wMeasureSpec);

        ViewManager.Params params = new ViewManager.Params();
        params.x = mViewManager.getWidth() / 4;
        params.y = mViewManager.getHeight() / 16;
        params.w = mViewManager.getWidth() / 2;
        params.h = 7 * mViewManager.getHeight() / 8;

        mViewManager.addModalAndFocusableView(view2, params);
    }

    public EditButtonCompanion(View editButton) {
        mViewManager = ViewManager.get();

        editButton.setOnClickListener(mOnEditClickListener);
    }
}
