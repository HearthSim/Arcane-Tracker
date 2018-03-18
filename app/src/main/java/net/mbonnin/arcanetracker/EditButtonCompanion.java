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

import net.mbonnin.arcanetracker.databinding.ImportDeckstringBinding;
import net.mbonnin.hsmodel.Card;

import java.util.ArrayList;

public class EditButtonCompanion {

    private ViewManager mViewManager = null;

    private View.OnClickListener mOnEditClickListener = v -> {
        View view = LayoutInflater.from(v.getContext()).inflate(R.layout.menu_view, null);

        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(wMeasureSpec, wMeasureSpec);

        int a[] = new int[2];
        v.getLocationOnScreen(a);

        new ChangeDeckCompanion(view.findViewById(R.id.changeDeck), v, () -> {mViewManager.removeView(view); return null;});


        view.findViewById(R.id.editDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);

            DeckEditorView.show(MainViewCompanion.Companion.getLegacyCompanion().getDeck());
        });

        view.findViewById(R.id.deleteDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);
            View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.delete_confirmation_view, null);
            view2.findViewById(R.id.deleteButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
                LegacyDeckList.INSTANCE.deleteDeck(MainViewCompanion.Companion.getLegacyCompanion().getDeck());
                ArrayList<Deck> list = LegacyDeckList.INSTANCE.get();
                Deck newDeck;

                if (!list.isEmpty()) {
                    newDeck = list.get(0);
                } else {
                    newDeck = LegacyDeckList.INSTANCE.createDeck(Card.CLASS_INDEX_WARRIOR);
                }
                MainViewCompanion.Companion.getLegacyCompanion().setDeck(newDeck);
            });
            view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
            });
            mViewManager.addCenteredView(view2, true);
        });

        view.findViewById(R.id.renameDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);

            Deck deck = MainViewCompanion.Companion.getLegacyCompanion().getDeck();
            if (deck.isArena()) {
                Toast.makeText(view.getContext(), "Sorry, you cannot rename the Arena deck !", Toast.LENGTH_LONG).show();
                return;
            }
            View view2 = LayoutInflater.from(v2.getContext()).inflate(R.layout.rename_deck_view, null);

            ((EditText) (view2.findViewById(R.id.editText))).setText(deck.name);
            view2.findViewById(R.id.renameButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
                deck.name = ((EditText) (view2.findViewById(R.id.editText))).getText().toString();
                MainViewCompanion.Companion.getLegacyCompanion().setDeck(deck);
                LegacyDeckList.INSTANCE.save();

            });
            view2.findViewById(R.id.cancelButton).setOnClickListener(v3 -> {
                mViewManager.removeView(view2);
            });
            mViewManager.addCenteredView(view2, true);
        });

        view.findViewById(R.id.createDeck).setOnClickListener(v2 -> {
            mViewManager.removeView(view);

            newDeckClicked(view.getContext());
        });

        ViewManager.Params params = new ViewManager.Params();
        params.setX(a[0]);
        params.setY(a[1] + v.getHeight() / 2 - view.getMeasuredHeight());
        params.setW(view.getMeasuredWidth());
        params.setH(view.getMeasuredHeight());

        mViewManager.addModalView(view, params);
    };

    private void newDeckClicked(Context context) {
        ImportDeckstringBinding binding = ImportDeckstringBinding.inflate(LayoutInflater.from(context));
        View view = binding.getRoot();

        String pasteData = getPasteData(context);
        if (pasteData != null && DeckString.INSTANCE.parse(pasteData) != null) {
            binding.editText.setText(pasteData);
        }

        binding.useDeckString.setOnClickListener(v -> {
            Deck deck = DeckString.INSTANCE.parse(binding.editText.getText().toString());
            if (deck != null) {
                mViewManager.removeView(view);
                LegacyDeckList.INSTANCE.addDeck(deck);
                MainViewCompanion.Companion.getLegacyCompanion().setDeck(deck);
            } else {
                Toast.makeText(context, Utils.INSTANCE.getString(R.string.cannotParseDeckstring), Toast.LENGTH_LONG).show();
            }
        });

        binding.createNewDeck.setOnClickListener(v -> {
            mViewManager.removeView(view);
            showNewDeckDialog(context);
        });

        ViewManager.Params params = new ViewManager.Params();
        ViewManager viewManager = ViewManager.Companion.get();

        params.setX(ViewManager.Companion.get().getWidth() / 8);
        params.setY(ViewManager.Companion.get().getHeight() / 16);
        params.setW(3 * ViewManager.Companion.get().getWidth() / 4);
        params.setH(7 * ViewManager.Companion.get().getHeight() / 8);

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
        params.setX(mViewManager.getWidth() / 4);
        params.setY(mViewManager.getHeight() / 16);
        params.setW(mViewManager.getWidth() / 2);
        params.setH(7 * mViewManager.getHeight() / 8);

        mViewManager.addModalAndFocusableView(view2, params);
    }

    public EditButtonCompanion(View editButton) {
        mViewManager = ViewManager.Companion.get();

        editButton.setOnClickListener(mOnEditClickListener);
    }
}
