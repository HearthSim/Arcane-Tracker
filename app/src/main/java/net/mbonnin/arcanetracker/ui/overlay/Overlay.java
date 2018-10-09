package net.mbonnin.arcanetracker.ui.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.mbonnin.arcanetracker.BuildConfig;
import net.mbonnin.arcanetracker.HDTApplication;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.ViewManager;
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion;

public class Overlay {
    private static Overlay sOverlay;

    public static Overlay get() {
        if (sOverlay == null) {
            sOverlay = new Overlay();
        }

        return sOverlay;
    }

    public void show() {
        MainViewCompanion.Companion.get().setState(MainViewCompanion.Companion.getSTATE_PLAYER(), false);
        MainViewCompanion.Companion.get().show(true);

        Context context = HDTApplication.Companion.getContext();

        int previousVersion = Settings.INSTANCE.get(Settings.INSTANCE.getVERSION(), 0);
        Settings.INSTANCE.set(Settings.INSTANCE.getVERSION(), BuildConfig.VERSION_CODE);

        if (Settings.INSTANCE.get(Settings.INSTANCE.getSHOW_CHANGELOG(), true)
                && previousVersion > 0
                && previousVersion < BuildConfig.VERSION_CODE) {
            View view = LayoutInflater.from(context).inflate(R.layout.whats_new, null);
            ViewManager.Params params = new ViewManager.Params();
            params.setX(ViewManager.Companion.get().getWidth() / 4);
            params.setY(ViewManager.Companion.get().getHeight() / 16);
            params.setW(ViewManager.Companion.get().getWidth() / 2);
            params.setH(7 * ViewManager.Companion.get().getHeight() / 8);

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
                    ViewManager.Companion.get().removeView(view);
                    Settings.INSTANCE.set(Settings.INSTANCE.getSHOW_CHANGELOG(), checkBox.isChecked());
                });
                ViewManager.Companion.get().addModalView(view, params);
            }
        }

        Onboarding.INSTANCE.start();
    }

    public void hide() {
        ViewManager.Companion.get().removeAllViews();
    }
}
