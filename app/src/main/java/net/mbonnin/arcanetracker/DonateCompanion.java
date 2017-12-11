package net.mbonnin.arcanetracker;

import android.content.Context;
import android.view.LayoutInflater;

import net.mbonnin.arcanetracker.databinding.DonateBinding;

class DonateCompanion {
    public DonateCompanion(DonateBinding binding) {

    }

    public static void show() {
        Context context = ArcaneTrackerApplication.Companion.getContext();
        ViewManager viewManager = ViewManager.get();

        DonateBinding binding = DonateBinding.inflate(LayoutInflater.from(context));

        new DonateCompanion(binding);

        ViewManager.Params params = new ViewManager.Params();
        params.x = viewManager.getWidth() / 4;
        params.y = viewManager.getHeight() / 16;
        params.w = viewManager.getWidth() / 2;
        params.h = 7 * viewManager.getHeight() / 8;

        viewManager.addModalAndFocusableView(binding.getRoot(), params);
    }
}
