package net.mbonnin.arcanetracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by martin on 10/19/16.
 */
public class DeckListAdapter extends RecyclerView.Adapter {
    OnDeckSelected onDeckSelectedListener;

    public interface OnDeckSelected {
        void onClick(Deck deck);
    }

    public void setOnDeckSelectedListener(OnDeckSelected listener) {
        onDeckSelectedListener = listener;

    }

    public DeckListAdapter() {
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ArcaneTrackerApplication.Companion.getContext()).inflate(R.layout.deck_line_view, null);
        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView;

        Deck deck;
        if (position >= LegacyDeckList.INSTANCE.get().size()) {
            deck = LegacyDeckList.INSTANCE.getArenaDeck();
        } else {
            deck = LegacyDeckList.INSTANCE.get().get(position);
        }

        view.setOnClickListener(v -> onDeckSelectedListener.onClick(deck));

        ((ImageView)(view.findViewById(R.id.deckImageRound))).setImageDrawable(Utils.INSTANCE.getDrawableForName(String.format("hero_%02d_round", deck.classIndex + 1)));
        ((TextView)(view.findViewById(R.id.deckName))).setText(deck.name);
    }

    @Override
    public int getItemCount() {
        return LegacyDeckList.INSTANCE.get().size() + 1;
    }
}
