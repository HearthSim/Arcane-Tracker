package net.mbonnin.arcanetracker.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;

import static android.view.View.GONE;

class DeckEntryHolder extends RecyclerView.ViewHolder {
    ImageView gift;
    ImageView background;
    TextView cost;
    TextView name;
    TextView count;
    View overlay;

    public DeckEntryHolder(View itemView) {
        super(itemView);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30));
        itemView.setLayoutParams(params);
        background = ((ImageView)(itemView.findViewById(R.id.background)));
        cost = ((TextView)itemView.findViewById(R.id.cost));
        name = ((TextView)itemView.findViewById(R.id.name));
        count = ((TextView)itemView.findViewById(R.id.count));
        overlay = itemView.findViewById(R.id.overlay);
        gift = (ImageView)itemView.findViewById(R.id.gift);
    }


    public void bind(DeckEntryItem entry) {
        Card card = entry.card;
        int c = entry.count;

        background.setImageDrawable(Utils.getDrawableForName("bar_" + card.id));
        cost.setText(card.cost + "");
        name.setText(card.name);
        count.setVisibility(GONE);
        gift.setVisibility(GONE);

        if (c > 0) {
            overlay.setBackgroundColor(Color.TRANSPARENT);

            if (Card.RARITY_LEGENDARY.equals(card.rarity)) {
                count.setVisibility(View.VISIBLE);
                count.setText("\u2605");
            } else if (c == 2) {
                count.setVisibility(View.VISIBLE);
                count.setText(c + "");
            } else if (entry.gift){
                gift.setVisibility(View.VISIBLE);
            }
        } else {
            overlay.setBackgroundColor(Color.argb(150, 0, 0, 0));
        }
    }
}
