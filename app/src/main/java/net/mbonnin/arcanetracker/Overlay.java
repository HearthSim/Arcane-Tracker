package net.mbonnin.arcanetracker;

import android.view.LayoutInflater;
import android.view.View;

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
        QuitDetector.get().start();

        MainService.start();

        int version = Settings.get(Settings.VERSION, 0);
        if (version != BuildConfig.VERSION_CODE) {
            Settings.set(Settings.VERSION, BuildConfig.VERSION_CODE);
            View view = LayoutInflater.from(ArcaneTrackerApplication.getContext()).inflate(R.layout.whats_new, null);
            ViewManager.Params params = new ViewManager.Params();
            params.x = ViewManager.get().getWidth() / 4;
            params.y = ViewManager.get().getHeight() / 16;
            params.w = ViewManager.get().getWidth() / 2;
            params.h = 7 * ViewManager.get().getHeight() / 8;

            view.findViewById(R.id.ok).setOnClickListener(v -> {
                ViewManager.get().removeView(view);
            });
            ViewManager.get().addModalView(view, params);
        }
    }

    public void hide() {
        QuitDetector.get().stop();
        ViewManager.get().removeAllViews();

        MainService.stop();
    }
}
