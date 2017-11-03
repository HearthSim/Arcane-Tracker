package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Overlay {
    private static Overlay sOverlay;

    public static Overlay get() {
        if (sOverlay == null) {
            sOverlay = new Overlay();
        }

        return sOverlay;
    }

    public void show() {
        MainViewCompanion.get().setState(MainViewCompanion.STATE_PLAYER, false);
        MainViewCompanion.get().show(true);

        Context context = ArcaneTrackerApplication.getContext();

        int previousVersion = Settings.get(Settings.VERSION, 0);
        Settings.set(Settings.VERSION, BuildConfig.VERSION_CODE);

        if (Settings.get(Settings.SHOW_CHANGELOG, true)
                && previousVersion > 0
                && previousVersion < BuildConfig.VERSION_CODE) {
            View view = LayoutInflater.from(context).inflate(R.layout.whats_new, null);
            ViewManager.Params params = new ViewManager.Params();
            params.x = ViewManager.get().getWidth() / 4;
            params.y = ViewManager.get().getHeight() / 16;
            params.w = ViewManager.get().getWidth() / 2;
            params.h = 7 * ViewManager.get().getHeight() / 8;

            CheckBox checkBox = view.findViewById(R.id.checkbox);
            checkBox.setChecked(true);

            int v = BuildConfig.VERSION_CODE;

            LinearLayout linearLayout = view.findViewById(R.id.changelog);
            int foundChangelog = 0;
            while (v > previousVersion) {
                int id = context.getResources().getIdentifier("changelog_" + v, "string", context.getPackageName());
                if (id > 0) {
                    String c = context.getString(id);
                    TextView textView = new TextView(context);
                    textView.setTextSize(24);
                    textView.setTextColor(Color.WHITE);
                    textView.setText(context.getString(R.string.cVersion, Integer.toString(v)));
                    textView.setTypeface(Typeface.DEFAULT_BOLD);

                    linearLayout.addView(textView);
                    foundChangelog++;

                    String items[] = c.split("\n");
                    for (String item: items) {
                        String s[] = item.split("\\{-\\}");
                        textView = new TextView(context);
                        textView.setTextSize(16);
                        textView.setTextColor(Color.WHITE);
                        textView.setText("• " + s[0]);
                        linearLayout.addView(textView);

                        if (s.length > 1) {
                            textView = new TextView(context);
                            textView.setTextSize(12);
                            textView.setTextColor(Color.WHITE);
                            textView.setText(s[1]);
                            linearLayout.addView(textView);
                        }
                    }
                }

                v--;
            }
            if (foundChangelog > 0) {
                view.findViewById(R.id.ok).setOnClickListener(unused -> {
                    ViewManager.get().removeView(view);
                    Settings.set(Settings.SHOW_CHANGELOG, checkBox.isChecked());
                });
                ViewManager.get().addModalView(view, params);
            }
        }
    }

    public void hide() {
        ViewManager.get().removeAllViews();
    }
}
