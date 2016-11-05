package net.mbonnin.arcanetracker;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by martin on 10/20/16.
 */

public class ClassAdapter extends RecyclerView.Adapter {
    private int mSelectedPosition;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.class_item_view, null);
        return new RecyclerView.ViewHolder(view) {};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView;
        ((ImageView)view.findViewById(R.id.imageView)).setImageDrawable(Utils.getDrawableForName(String.format("hero_%02d_round", position + 1)));
        ((TextView)view.findViewById(R.id.className)).setText(Card.classNameList[position]);

        view.setOnClickListener(v -> {
            mSelectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
        });

        if (position == mSelectedPosition) {
            view.setBackgroundColor(view.getContext().getResources().getColor(R.color.colorPrimary));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

    }

    @Override
    public int getItemCount() {
        return Card.classNameList.length;
    }
    
    public int getSelectedClassIndex() {
        return mSelectedPosition;
    }
}
