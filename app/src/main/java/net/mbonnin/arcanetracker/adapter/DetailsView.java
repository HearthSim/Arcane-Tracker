package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.mbonnin.arcanetracker.CardRenderer;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Typefaces;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Entity;

public class DetailsView extends LinearLayout{
    public DetailsView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);

    }

    public void configure(Bitmap bitmap, DeckEntryItem deckEntryItem, int height) {

        int w = (height * CardRenderer.TOTAL_WIDTH)/CardRenderer.TOTAL_HEIGHT;
        if (bitmap != null) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(bitmap);
            LayoutParams layoutParams = new LayoutParams(w, height);
            addView(imageView, layoutParams);
        }

        int i = 0;
        for (Entity entity: deckEntryItem.entityList) {
            LayoutParams layoutParams = new LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            int p = Utils.dpToPx(5);
            layoutParams.leftMargin = layoutParams.rightMargin = p;
            layoutParams.topMargin = Utils.dpToPx(30);

            TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.extra_textview, null);

            StringBuilder builder = new StringBuilder();

            String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
            builder.append(getContext().getString(R.string.card, entity.EntityID));
            builder.append("\n");
            builder.append("\n");
            if (entity.extra.drawTurn != -1) {
                builder.append(getContext().getString(R.string.drawnTurn, entity.extra.drawTurn));
                if (entity.extra.mulliganed) {
                    builder.append(" (");
                    builder.append(getContext().getString(R.string.mulliganed));
                    builder.append(")");
                }
                builder.append("\n");
            } else {
                builder.append(getContext().getString(R.string.inDeck));
                builder.append("\n");
            }
            if (entity.extra.playTurn != -1) {
                builder.append(getContext().getString(R.string.playedTurn, entity.extra.playTurn));
                builder.append("\n");
            }
            if (entity.extra.diedTurn != -1 && (Entity.CARDTYPE_MINION.equals(cardType) || Entity.CARDTYPE_WEAPON.equals(cardType))) {
                builder.append(getContext().getString(R.string.diedTurn, entity.extra.diedTurn));
                builder.append("\n");
            }
            String s = builder.toString();

            textView.setText(s);

            textView.setTypeface(Typefaces.franklin());

            addView(textView, layoutParams);
            i++;
        }

    }
}
