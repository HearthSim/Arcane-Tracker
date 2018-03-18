package net.mbonnin.arcanetracker;

import android.content.Context;
import android.view.LayoutInflater;

import net.mbonnin.arcanetracker.databinding.DonateBinding;

class DonateCompanion {
    public DonateCompanion(DonateBinding binding) {

    }

    public static void show() {
        Context context = ArcaneTrackerApplication.Companion.getContext();
        ViewManager viewManager = ViewManager.Companion.get();

        DonateBinding binding = DonateBinding.inflate(LayoutInflater.from(context));

        new DonateCompanion(binding);

        ViewManager.Params params = new ViewManager.Params();
        params.setX(ViewManager.Companion.get().getWidth() / 4);
        params.setY(ViewManager.Companion.get().getHeight() / 16);
        params.setW(ViewManager.Companion.get().getWidth() / 2);
        params.setH(7 * ViewManager.Companion.get().getHeight() / 8);

        viewManager.addModalAndFocusableView(binding.getRoot(), params);
    }
}
