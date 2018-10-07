package net.mbonnin.arcanetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import net.mbonnin.arcanetracker.databinding.DonateBinding;
import net.mbonnin.arcanetracker.extension.ActivityExtensionsKt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

import static android.view.View.GONE;

public class SupportActivity extends AppCompatActivity {

    private DonateBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = DonateBinding.inflate(LayoutInflater.from(this));

        setContentView(binding.getRoot());

        binding.cardView.setOnClickListener(v -> Timber.d("click"));
        binding.getRoot().setOnClickListener(v -> finish());

        binding.buttonsContainer.setVisibility(View.INVISIBLE);
        binding.iabError.setVisibility(GONE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityExtensionsKt.makeFullscreen(this);
    }
}
