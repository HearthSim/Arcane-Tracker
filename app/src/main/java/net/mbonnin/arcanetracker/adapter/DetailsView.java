package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.mbonnin.arcanetracker.CardRenderer;
import net.mbonnin.arcanetracker.CardUtil;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Typefaces;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.databinding.DetailsViewBinding;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.hsmodel.CardId;
import net.mbonnin.hsmodel.PlayerClass;
import net.mbonnin.hsmodel.Type;

import java.util.ArrayList;
import java.util.List;

public class DetailsView extends LinearLayout {
    private int mTopMargin;
    private int mCardWidth;

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

        mCardWidth = w;
        mTopMargin = 30;

        for (Entity entity : deckEntryItem.entityList) {
            DetailsViewBinding b = DetailsViewBinding.inflate(LayoutInflater.from(getContext()));

            StringBuilder builder = new StringBuilder();

            if (Utils.INSTANCE.isAppDebuggable()) {
                builder.append(getContext().getString(R.string.card, entity.EntityID));
                builder.append("\n");
            }


            String cardType = entity.tags.get(Entity.KEY_CARDTYPE);
            if (entity.extra.drawTurn != -1) {
                builder.append(getContext().getString(R.string.drawnTurn, GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn)));
                if (entity.extra.mulliganed) {
                    builder.append(" (");
                    builder.append(getContext().getString(R.string.mulliganed));
                    builder.append(")");
                }
                builder.append("\n");
            }

            if (entity.extra.playTurn != -1) {
                builder.append(getContext().getString(R.string.playedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.playTurn)));
                builder.append("\n");
            }
            if (entity.extra.diedTurn != -1 && (Type.MINION.equals(cardType) || Type.WEAPON.equals(cardType))) {
                builder.append(getContext().getString(R.string.diedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.diedTurn)));
                builder.append("\n");
            }
            if (!TextUtils.isEmpty(entity.extra.createdBy)) {
                builder.append(getContext().getString(R.string.createdBy, CardUtil.INSTANCE.getCard(entity.extra.createdBy).name));
            }

            if (Entity.ZONE_SECRET.equals(entity.tags.get(Entity.KEY_ZONE))
                    && TextUtils.isEmpty(entity.CardID)) {
                builder.append(Utils.INSTANCE.getString(R.string.possibleSecrets));
                appendPossibleSecrets((LinearLayout) b.getRoot(), entity);
            }

            String s = builder.toString();

            if (Utils.INSTANCE.isEmpty(s)) {
                builder.append(Utils.INSTANCE.getString(R.string.inDeck));
                builder.append("\n");
                s = builder.toString();
            }

            b.textView.setText(s);
            b.textView.setTypeface(Typefaces.franklin());

            addView(b.getRoot());
        }

        applyMargins();
    }

    void applyMargins() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ImageView) {
                continue;
            }
            LayoutParams layoutParams = new LayoutParams(mCardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            int p = Utils.INSTANCE.dpToPx(5);
            layoutParams.leftMargin = layoutParams.rightMargin = p;
            layoutParams.topMargin = Utils.INSTANCE.dpToPx(mTopMargin);
            child.setLayoutParams(layoutParams);
        }
        requestLayout();
    }

    public void setTopMargin(int topMargin) {
        mTopMargin = topMargin;
        applyMargins();
    }

    private void appendPossibleSecrets(LinearLayout verticalLayout, Entity entity) {
        String playerClass = entity.tags.get(Entity.KEY_CLASS);

        if (playerClass == null) {
            return;
        }

        List<DeckEntryItem> list = new ArrayList<>();

        switch (playerClass) {
            case PlayerClass.HUNTER:
                addSecret(list, CardId.BEAR_TRAP, entity.extra.selfHeroAttacked);
                addSecret(list, CardId.CAT_TRICK, entity.extra.otherPlayerCastSpell);
                addSecret(list, CardId.EXPLOSIVE_TRAP, entity.extra.selfHeroAttacked);
                addSecret(list, CardId.FREEZING_TRAP, entity.extra.selfHeroAttacked || entity.extra.selfMinionWasAttacked);
                addSecret(list, CardId.SNAKE_TRAP, entity.extra.selfMinionWasAttacked);
                addSecret(list, CardId.SNIPE, entity.extra.otherPlayerPlayedMinion);
                addSecret(list, CardId.DART_TRAP, entity.extra.otherPlayerHeroPowered);
                addSecret(list, CardId.HIDDEN_CACHE, entity.extra.otherPlayerPlayedMinion);
                addSecret(list, CardId.MISDIRECTION, entity.extra.selfHeroAttacked);
                addSecret(list, CardId.VENOMSTRIKE_TRAP, entity.extra.selfMinionWasAttacked);
                break;
            case PlayerClass.MAGE:
                addSecret(list, CardId.MIRROR_ENTITY, entity.extra.otherPlayerPlayedMinion);
                addSecret(list, CardId.MANA_BIND, entity.extra.otherPlayerCastSpell);
                addSecret(list, CardId.COUNTERSPELL, entity.extra.otherPlayerCastSpell);
                addSecret(list, CardId.EFFIGY, entity.extra.selfPlayerMinionDied);
                addSecret(list, CardId.ICE_BARRIER, entity.extra.selfHeroAttacked);
                addSecret(list, CardId.POTION_OF_POLYMORPH, entity.extra.otherPlayerPlayedMinion);
                addSecret(list, CardId.DUPLICATE, entity.extra.selfPlayerMinionDied);
                addSecret(list, CardId.VAPORIZE, entity.extra.selfHeroAttackedByMinion);
                addSecret(list, CardId.ICE_BLOCK, false);
                addSecret(list, CardId.SPELLBENDER, entity.extra.selfMinionTargetedBySpell);
                addSecret(list, CardId.FROZEN_CLONE, entity.extra.otherPlayerPlayedMinion);
                break;
            case PlayerClass.PALADIN:
                addSecret(list, CardId.COMPETITIVE_SPIRIT, entity.extra.competitiveSpiritTriggerConditionHappened);
                addSecret(list, CardId.AVENGE, entity.extra.selfPlayerMinionDied);
                addSecret(list, CardId.REDEMPTION, entity.extra.selfPlayerMinionDied);
                addSecret(list, CardId.REPENTANCE, entity.extra.otherPlayerPlayedMinion);
                addSecret(list, CardId.SACRED_TRIAL, entity.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready);
                addSecret(list, CardId.NOBLE_SACRIFICE, entity.extra.selfHeroAttacked || entity.extra.selfMinionWasAttacked);
                addSecret(list, CardId.GETAWAY_KODO, entity.extra.selfPlayerMinionDied);
                addSecret(list, CardId.EYE_FOR_AN_EYE, entity.extra.selfHeroAttacked);
                break;
        }

        int i = 0;
        LinearLayout horizontalLayout = null;
        for (DeckEntryItem deckEntryItem: list) {
            if (i%2 == 0) {
                horizontalLayout = new LinearLayout(getContext());
                horizontalLayout.setOrientation(HORIZONTAL);
                //verticalLayout.addView(horizontalLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30)));
            }

            View view = LayoutInflater.from(getContext()).inflate(R.layout.bar_card, null);
            ViewGroup barTemplate = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.bar_template, null);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            barTemplate.addView(view, 0, params);

            DeckEntryHolder holder = new DeckEntryHolder(barTemplate);
            holder.bind(deckEntryItem);

            verticalLayout.addView(barTemplate, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void addSecret(List<DeckEntryItem> list, String cardId, boolean condition) {
        DeckEntryItem deckEntryItem = new DeckEntryItem();
        deckEntryItem.card = CardUtil.INSTANCE.getCard(cardId);
        deckEntryItem.count = condition ? 0 : 1;
        list.add(deckEntryItem);
    }
}
