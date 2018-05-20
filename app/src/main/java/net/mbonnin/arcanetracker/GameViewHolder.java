package net.mbonnin.arcanetracker;

import android.content.Context;
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
        hero = itemView.findViewById(R.id.hero);
        opponentHero = itemView.findViewById(R.id.opponentHero);
        deckName = itemView.findViewById(R.id.deckName);
        winLoss = itemView.findViewById(R.id.winLoss);
        coin = itemView.findViewById(R.id.coin);
        opponentName = itemView.findViewById(R.id.opponentName);
    }

    public void bind(GameSummary summary) {
        Context context = itemView.getContext();
        hero.setImageDrawable(Utils.INSTANCE.getDrawableForNameDeprecated(String.format("hero_%02d_round", summary.hero + 1)));
        opponentHero.setImageDrawable(Utils.INSTANCE.getDrawableForNameDeprecated(String.format("hero_%02d_round", summary.opponentHero + 1)));
        deckName.setText(summary.deckName);
        winLoss.setText(summary.win ? context.getString(R.string.win):context.getString(R.string.loss));
        coin.setVisibility(summary.coin ? View.VISIBLE:View.INVISIBLE);
        opponentName.setText(HeroUtilKt.getDisplayName(summary.opponentHero));
    }


}
