package net.mbonnin.arcanetracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import net.mbonnin.arcanetracker.databinding.DonateBinding;
import net.mbonnin.arcanetracker.extension.ActivityExtensionsKt;

import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DonateActivity extends AppCompatActivity {
    private static final int ACTIVITY_RESULT_IAB_FINISHED = 42;

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = (result, info) -> {
        if (result.isSuccess()) {
            InAppBilling.get().getPurchaseMap().put(info.getSku(), info);
            updateButtons();
        }

        Timber.d("error is " + result.getResponse());

        if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
            // the user just pressed outside the popup, fail silently
            return;
        }
        displayDialog(result.isSuccess() ? R.string.thank_you : R.string.purchase_failed);
    };
    private IabHelper.OnConsumeFinishedListener mConsumeFinishListener = (purchase, result) -> {
        if (result.isSuccess()) {
            InAppBilling.get().getPurchaseMap().remove(purchase.getSku());
            updateButtons();
        }
    };
    private Inventory mInventory;

    private void displayDialog(int resId) {
        try {
            new AlertDialog.Builder(DonateActivity.this)
                    .setTitle(getString(resId))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void updateButtons() {
        List<Button> buttonList = Arrays.asList(binding.holyLight, binding.blessingOfKings, binding.dinosize);
        List<String> skuList = Arrays.asList(InAppBilling.SKU_HOLY_LIGHT, InAppBilling.SKU_BLESSING_OF_KINGS, InAppBilling.SKU_DINOSIZE);
        List<Integer> textIdList = Arrays.asList(R.string.holy_light, R.string.blessing_of_kings, R.string.dinosize);
        List<Integer> purchasedIdList = Arrays.asList(R.string.holy_light_purchased, R.string.blessing_of_kings_purchased, R.string.dinosize_purchased);
        List<Integer> compoundIdList = Arrays.asList(R.drawable.sun, R.drawable.king, R.drawable.dino);


        for (int i = 0; i < skuList.size(); i++) {
            String text = getString(textIdList.get(i), mInventory.getSkuDetails(skuList.get(i)).getPrice());
            text = text.toUpperCase();

            String sku = skuList.get(i);

            buttonList.get(i).setTypeface(Typeface.DEFAULT_BOLD);
            buttonList.get(i).setTransformationMethod(null);

            Purchase purchase = InAppBilling.get().getPurchaseMap().get(sku);
            if (purchase != null) {
                buttonList.get(i).setText(getString(purchasedIdList.get(i)));

                if (true) {
                    // do not allow users to consume purchases. They might give some priviledges
                    // at some point
                    buttonList.get(i).setOnClickListener(null);
                    buttonList.get(i).setEnabled(false);
                } else {
                    buttonList.get(i).setOnClickListener(v -> {
                        Timber.d("consume");
                        try {
                            InAppBilling.get().consume(purchase, mConsumeFinishListener);
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            e.printStackTrace();
                        }
                    });
                }

            } else {
                Spannable buttonLabel = new SpannableString("  " + text);
                Drawable d = getResources().getDrawable(compoundIdList.get(i));
                int size = (int) (buttonList.get(i).getTextSize() * 1.4f);
                d.setBounds(0, 0, size, size);

                buttonLabel.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                buttonList.get(i).setText(buttonLabel);

                buttonList.get(i).setEnabled(true);
                buttonList.get(i).setOnClickListener(v -> {
                    Timber.d("launchPurchaseFlow");
                    try {
                        InAppBilling.get().launchPurchaseFlow(this, sku, ACTIVITY_RESULT_IAB_FINISHED, mPurchaseFinishedListener);
                    } catch (Exception e) {
                        Timber.e(e);
                        displayDialog(R.string.purchase_failed);
                    }
                });
            }
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
        mInventory = inventory;
        binding.buttonsContainer.setVisibility(View.VISIBLE);
        binding.buttonsContainer.setEnabled(false);
        binding.progressBar.setVisibility(GONE);

        updateButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityExtensionsKt.makeFullscreen(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_RESULT_IAB_FINISHED) {
            InAppBilling.get().handleActivityResult(requestCode, resultCode, data);
        }
    }
}
