package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by martin on 10/20/16.
 */

public class CreateDeckView extends CardView {
    public CreateDeckView(Context context) {
        super(context);
    }

    public CreateDeckView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static CreateDeckView create(Context context) {
        return (CreateDeckView) LayoutInflater.from(context).inflate(R.layout.create_deck_view, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        Button button = (Button)findViewById(R.id.button);
        EditText editText = (EditText)findViewById(R.id.editText);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        ClassAdapter adapter = new ClassAdapter();
        recyclerView.setAdapter(adapter);

        button.setOnClickListener(v -> {
            String name = editText.getText().toString();
            if (name.equals("") || name == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.please_enter_a_name), Toast.LENGTH_LONG).show();
                return;
            }

            Deck deck = DeckList.createDeck(adapter.getSelectedClassIndex());
            deck.name = name;

            MainViewCompanion.getPlayerCompanion().setDeck(deck);

            ViewManager viewManager = ViewManager.get();

            viewManager.removeView(CreateDeckView.this);

            DeckEditorView deckEditorView = DeckEditorView.build(getContext());
            deckEditorView.setDeck(deck);
            ViewManager.Params params = new ViewManager.Params();
            params.x = 0;
            params.y = 0;
            params.w = viewManager.getWidth();
            params.h = viewManager.getHeight();
            viewManager.addView(deckEditorView, params);

        });
    }
}
