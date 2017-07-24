package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.mbonnin.arcanetracker.model.GameSummary;

public class GameViewHolder extends RecyclerView.ViewHolder {
    private final View coin;
    ImageView hero;
    ImageView opponentHero;
    TextView deckName;
    TextView winLoss;
    TextView opponentName;

    public GameViewHolder(View itemView) {
        super(itemView);
        hero = (ImageView) itemView.findViewById(R.id.hero);
        opponentHero = (ImageView) itemView.findViewById(R.id.opponentHero);
        deckName = (TextView) itemView.findViewById(R.id.deckName);
        winLoss = (TextView) itemView.findViewById(R.id.winLoss);
        coin = itemView.findViewById(R.id.coin);
        opponentName = (TextView) itemView.findViewById(R.id.opponentName);
    }

    public void bind(GameSummary summary) {
        Context context = itemView.getContext();
        hero.setImageDrawable(Utils.getDrawableForName(String.format("hero_%02d_round", summary.hero + 1)));
        opponentHero.setImageDrawable(Utils.getDrawableForName(String.format("hero_%02d_round", summary.opponentHero + 1)));
        deckName.setText(summary.deckName);
        winLoss.setText(summary.win ? context.getString(R.string.win):context.getString(R.string.loss));
        coin.setVisibility(summary.coin ? View.VISIBLE:View.INVISIBLE);
        opponentName.setText(Card.classIndexToNiceName(summary.opponentHero));
    }


}
