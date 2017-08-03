package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.CardRenderer;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Typefaces;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.databinding.DetailsViewBinding;
import net.mbonnin.arcanetracker.parser.Entity;

import java.util.ArrayList;
import java.util.List;

public class DetailsView extends LinearLayout {
    public DetailsView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);

    }

    public void configure(Bitmap bitmap, DeckEntryItem deckEntryItem, int height) {

        int w = (height * CardRenderer.TOTAL_WIDTH) / CardRenderer.TOTAL_HEIGHT;
        if (bitmap != null) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(bitmap);
            LayoutParams layoutParams = new LayoutParams(w, height);
            addView(imageView, layoutParams);
        }

        int i = 0;
        for (Entity entity : deckEntryItem.entityList) {
            LayoutParams layoutParams = new LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            int p = Utils.dpToPx(5);
            layoutParams.leftMargin = layoutParams.rightMargin = p;
            layoutParams.topMargin = Utils.dpToPx(30);

            DetailsViewBinding b = DetailsViewBinding.inflate(LayoutInflater.from(getContext()));
            TextView textView = b.textView;

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

            b.textView.setText(s);
            b.textView.setTypeface(Typefaces.franklin());

            if (Entity.ZONE_SECRET.equals(entity.tags.get(Entity.KEY_ZONE))
                    && TextUtils.isEmpty(entity.CardID)) {
                appendPossibleSecrets((LinearLayout) b.getRoot(), entity);
            }

            addView(b.getRoot(), layoutParams);
            i++;
        }

    }

    private void appendPossibleSecrets(LinearLayout linearLayout, Entity entity) {
        String clazz = entity.tags.get(Entity.KEY_CLASS);

        if (clazz == null) {
            return;
        }

        int classIndex = Card.niceNameToClassIndexNC(clazz);

        List<DeckEntryItem> list = new ArrayList<>();

        switch (classIndex) {
            case Card.CLASS_INDEX_HUNTER:
                addSecret(list, Card.BEAR_TRAP, entity.extra.opponentHeroWasAttacked);
                addSecret(list, Card.CAT_TRICK, entity.extra.spellPlayed);
                addSecret(list, Card.EXPLOSIVE_TRAP, entity.extra.opponentHeroWasAttacked);
                addSecret(list, Card.FREEZING_TRAP, entity.extra.opponentHeroWasAttacked || entity.extra.minonWasAttacked);
                addSecret(list, Card.SNAKE_TRAP, entity.extra.minonWasAttacked);
                addSecret(list, Card.SNIPE, entity.extra.minionPlayed);
                addSecret(list, Card.DART_TRAP, entity.extra.heroPowerPlayed);
                addSecret(list, Card.HIDDEN_CACHE, entity.extra.minionPlayed);
                addSecret(list, Card.MISDIRECTION, entity.extra.opponentHeroWasAttacked);
                break;
            case Card.CLASS_INDEX_MAGE:
                addSecret(list, Card.MIRROR_ENTITY, entity.extra.minionPlayed);
                addSecret(list, Card.MANA_BIND, entity.extra.spellPlayed);
                addSecret(list, Card.COUNTERSPELL, entity.extra.spellPlayed);
                addSecret(list, Card.EFFIGY, entity.extra.minionPlayed);
                addSecret(list, Card.ICE_BARRIER, entity.extra.opponentHeroWasAttacked);
                addSecret(list, Card.POTION_OF_POLYMORPH, entity.extra.minionPlayed);
                addSecret(list, Card.DUPLICATE, entity.extra.opponentMinionDied);
                addSecret(list, Card.VAPORIZE, entity.extra.opponentHeroWasAttacked);
                //addSecret(list, Card.ICE_BLOCK, entity.extra.opponentHeroWasAttacked);
                //addSecret(list, Card.SPELL_BENDER, entity.extra.opponentHeroWasAttacked);
                break;
            case Card.CLASS_INDEX_PALADIN:
                //addSecret(list, Card.COMPETITIVE_SPIRIT, entity.extra.minionPlayed);
                addSecret(list, Card.AVENGE, entity.extra.opponentMinionDied);
                addSecret(list, Card.REDEMPTION, entity.extra.opponentMinionDied);
                addSecret(list, Card.REPENTANCE, entity.extra.minionPlayed);
                //addSecret(list, Card.SACRED_TRIAL, entity.extra.opponentHeroWasAttacked);
                addSecret(list, Card.NOBLE_SACRIFIC, entity.extra.opponentHeroWasAttacked || entity.extra.minonWasAttacked);
                addSecret(list, Card.GETAWAY_KOD, entity.extra.opponentMinionDied);
                addSecret(list, Card.EYE_FOR_EYE, entity.extra.opponentHeroWasAttacked);
                break;
        }

        for (DeckEntryItem deckEntryItem: list) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.bar_card, null);
            ViewGroup barTemplate = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.bar_template, null);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            barTemplate.addView(view, 0, params);

            DeckEntryHolder holder = new DeckEntryHolder(barTemplate);
            holder.bind(deckEntryItem);

            linearLayout.addView(barTemplate, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30)));
        }
    }

    private void addSecret(List<DeckEntryItem> list, String cardId, boolean condition) {
        DeckEntryItem deckEntryItem = new DeckEntryItem();
        deckEntryItem.card = CardDb.getCard(cardId);
        deckEntryItem.count = condition ? 0 : 1;
        list.add(deckEntryItem);
    }
}
