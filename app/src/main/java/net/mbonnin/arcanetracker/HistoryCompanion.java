package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.model.GameSummary;

public class HistoryCompanion {
    public HistoryCompanion(View view) {
        view.findViewById(R.id.historyEmpty);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        TextView historyEmpty = view.findViewById(R.id.historyEmpty);
        Button eraseHistory = view.findViewById(R.id.eraseHistory);

        GameAdapter adapter = new GameAdapter(GameSummary.getGameSummary());
        adapter.setOnclickListener(summary -> {
            if (summary.hsreplayUrl == null) {
                Context context = view.getContext();
                Toast.makeText(context, context.getString(R.string.could_not_find_replay), Toast.LENGTH_LONG).show();
                return;
            }
            ViewManager.get().removeView(view);
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(summary.hsreplayUrl));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ArcaneTrackerApplication.Companion.getContext().startActivity(i);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(ArcaneTrackerApplication.Companion.getContext()));
        recyclerView.setAdapter(adapter);

        historyEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        eraseHistory.setOnClickListener(v -> {
            GameSummary.eraseGameSummary();
            adapter.notifyDataSetChanged();
            historyEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });
    }

    public static void show() {
        Context context = ArcaneTrackerApplication.Companion.getContext();
        ViewManager viewManager = ViewManager.get();
        View view = LayoutInflater.from(context).inflate(R.layout.history_view, null);

        new HistoryCompanion(view);

        ViewManager.Params params = new ViewManager.Params();
        params.x = viewManager.getWidth() / 4;
        params.y = viewManager.getHeight() / 16;
        params.w = viewManager.getWidth() / 2;
        params.h = 7 * viewManager.getHeight() / 8;

        viewManager.addModalAndFocusableView(view, params);

    }
}
