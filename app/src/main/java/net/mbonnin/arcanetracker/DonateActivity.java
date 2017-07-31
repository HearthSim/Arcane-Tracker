package net.mbonnin.arcanetracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import net.mbonnin.arcanetracker.databinding.DonateBinding;

import java.util.Arrays;
import java.util.HashMap;
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
    private HashMap<String, Purchase> mPurchases = new HashMap<>();

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = (result, info) -> {
        if (result.isSuccess()){
            mPurchases.put(info.getSku(), info);
        }
        displayDialog(result.isSuccess() ? R.string.thank_you : R.string.purchase_failed);
    };

    private void displayDialog(int resId) {
        try {
            new AlertDialog.Builder(DonateActivity.this)
                    .setTitle(getString(resId))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        finish();
                    })
                    .show();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

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
                Timber.e(e);
                onInventoryFailed();
            }

        }, e -> {
            Timber.e(e);
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
        List<Integer> compoundId = Arrays.asList(R.drawable.sun, R.drawable.king, R.drawable.dino);

        for (Purchase purchase: inventory.getAllPurchases()) {
            mPurchases.put(purchase.getSku(), purchase);
        }

        for (int i = 0; i < sku.size(); i++) {
            String text = getContext().getString(textId.get(i), inventory.getSkuDetails(sku.get(i)).getPrice());
            text = text.toUpperCase();

            Spannable buttonLabel = new SpannableString("  " + text);
            Drawable d = getResources().getDrawable(compoundId.get(i));
            int size = (int) (button.get(i).getTextSize() * 1.4f);
            d.setBounds(0, 0, size, size);

            button.get(i).setTransformationMethod(null);
            buttonLabel.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            button.get(i).setText(buttonLabel);
            button.get(i).setTypeface(Typeface.DEFAULT_BOLD);

            String finalSku = sku.get(i);
            button.get(i).setOnClickListener(v -> {
                ViewManager.get().removeView(binding.getRoot());

                if (mPurchases.get(finalSku) != null) {
                    try {
                        InAppBilling.get().getHelper().consumeAsync(inventory.getPurchase(finalSku), (purchase, result) -> {
                            if (result.isSuccess()) {
                                mPurchases.remove(finalSku);
                                launchPurchase(finalSku);
                            } else {
                                displayDialog(R.string.purchase_failed);
                            }
                        });
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        e.printStackTrace();
                        displayDialog(R.string.purchase_failed);
                    }
                } else {
                    launchPurchase(finalSku);
                }
            });
        }
    }

    private void launchPurchase(String finalSku) {
        Timber.d("launchPurchaseFlow");
        try {
            InAppBilling.get().getHelper().launchPurchaseFlow(this, finalSku, ACTIVITY_RESULT_IAB_FINISHED, mPurchaseFinishedListener);
        } catch (Exception e) {
            Timber.e(e);
            displayDialog(R.string.purchase_failed);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_RESULT_IAB_FINISHED) {
            InAppBilling.get().getHelper().handleActivityResult(requestCode, resultCode, data);
        }
    }
}
