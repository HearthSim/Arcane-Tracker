package net.mbonnin.arcanetracker;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.mbonnin.arcanetracker.model.GameSummary;

import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter {

    private final List<GameSummary> mGameList;
    private Listener mListener;

    public interface Listener {
        void onClick(GameSummary summary);
    }
    public GameAdapter(List<GameSummary> gameList) {
        mGameList = gameList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_view, null);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((GameViewHolder)holder).bind((mGameList.get(position)));
        holder.itemView.setOnClickListener(v -> mListener.onClick(mGameList.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return mGameList.size();
    }

    public void setOnclickListener(Listener listener) {
        mListener = listener;
    }
}
