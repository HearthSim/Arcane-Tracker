package net.mbonnin.arcanetracker;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;

import net.mbonnin.arcanetracker.databinding.DonateBinding;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.VISIBLE;
import static net.mbonnin.arcanetracker.ArcaneTrackerApplication.getContext;

public class DonateActivity extends AppCompatActivity {
    private static final int ACTIVITY_RESULT_IAB_FINISHED = 42;
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = (result, info) -> {

        String text = getString(result.isSuccess() ? R.string.thank_you: R.string.purchase_failed);

        new AlertDialog.Builder(getContext())
                .setTitle(text)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    finish();
                })
                .show();
    };
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

        InAppBilling.get().getInventory().subscribe(inventory -> {
            try {
                onInventory(inventory);
            } catch (Exception e) {
                onInventoryFailed();
            }

        }, e -> {
            onInventoryFailed();
        });
    }

    private void onInventoryFailed() {
        binding.progressBar.setVisibility(GONE);
        binding.buttonsContainer.setVisibility(View.INVISIBLE);
        binding.iabError.setVisibility(VISIBLE);
    }

    private void onInventory(Inventory inventory) {
        binding.buttonsContainer.setVisibility(View.VISIBLE);
        binding.buttonsContainer.setEnabled(false);
        binding.progressBar.setVisibility(GONE);

        List<Button> button = Arrays.asList(binding.holyLight, binding.blessingOfKings, binding.dinosize);
        List<String> sku = Arrays.asList(InAppBilling.SKU_HOLY_LIGHT, InAppBilling.SKU_BLESSING_OF_KINGS, InAppBilling.SKU_DINOSIZE);
        List<Integer> textId = Arrays.asList(R.string.holy_light, R.string.blessing_of_kings, R.string.dinosize);

        for (int i = 0; i < sku.size(); i++) {
            button.get(i).setText(getContext().getString(textId.get(i), inventory.getSkuDetails(sku.get(i)).getPrice()));
            int finalI = i;
            button.get(i).setOnClickListener(v->{
                ViewManager.get().removeView(binding.getRoot());
                try {
                    Timber.d("launchPurchaseFlow");
                    InAppBilling.get().getHelper().launchPurchaseFlow(this, sku.get(finalI), ACTIVITY_RESULT_IAB_FINISHED, mPurchaseFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    |SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        decorView.setSystemUiVisibility(uiOptions);
    }
}
