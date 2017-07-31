package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import rx.Scheduler;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class InAppBilling {
    private final Single<Inventory> mCache;
    private final Scheduler mScheduler;
    private Map<String, Purchase> mPurchaseMap = new HashMap<>();
    private IabHelper mHelper;

    public static final String SKU_HOLY_LIGHT = "holy_light";
    public static final String SKU_BLESSING_OF_KINGS = "blessing_of_kings";
    public static final String SKU_DINOSIZE = "dinosize";

    private static InAppBilling sInAppBilling;

    public static InAppBilling get() {
        if (sInAppBilling == null) {
            sInAppBilling = new InAppBilling(ArcaneTrackerApplication.getContext());
        }
        return sInAppBilling;
    }

    private boolean mSetupError;

    volatile private boolean mSetupFinished;
    private final Object mLock = new Object();

    IabHelper.OnIabSetupFinishedListener setupFinishedListener = result -> {
        if (!result.isSuccess()) {
            // Oh no, there was a problem.
            Timber.d("Problem setting up In-app Billing: " + result);
            mSetupError = true;
        } else {
            Timber.d("Hooray, IAB is fully set up ");
        }
        synchronized (mLock) {
            mSetupFinished = true;
            mLock.notifyAll();
        }
    };

    public InAppBilling(Context context) {
        mScheduler = Schedulers.from(Executors.newSingleThreadExecutor());
        mCache = Single.fromCallable(() -> {

            long startTime = System.currentTimeMillis();
            synchronized (mLock) {
                while (!mSetupFinished && System.currentTimeMillis() - startTime < 30000) {
                    mLock.wait();
                }
            }

            if (!mSetupFinished || mSetupError) {
                throw new Exception("IAB Setup Error");
            }

            ArrayList<String> skuList = new ArrayList<>();
            skuList.add(SKU_HOLY_LIGHT);
            skuList.add(SKU_BLESSING_OF_KINGS);
            skuList.add(SKU_DINOSIZE);

            return mHelper.queryInventory(true, skuList, null);
        }).subscribeOn(mScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(inventory -> {
                    for (Purchase purchase: inventory.getAllPurchases()) {
                        mPurchaseMap.put(purchase.getSku(), purchase);
                    }
                })
                .cache()
                .observeOn(AndroidSchedulers.mainThread());

        mHelper = new IabHelper(context, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoYhDA6fIn/7GLTass2NT6fWYGP6ZZ+nBxEdn5zMY+fgGfthup2R8J5N8Is/nKke0GujqWyLUafyPiutGHMARGTkaTo2PmedBFMMMMShAyYVciP7V7lByk8leqKbwWjoSw69YzYeeIJUqeEB/PALLf05Gn8jRAXwwknb2+GgIc7sW9B/QzcwJuj21mk6TAAmNZhzcrIe4S3hi73+VtmXToQaHJMb464JWq5xScyI+NIhpFLMgZotsBwI8sD1nnQnAFrLSyjUU0891dz0nTbzdTb1FacwFTZM+qpqVSf5rnI6nS22dMkOne2q9C0336COUGEIVhwIJCxN6O0EbQ22ZpwIDAQAB");
        mHelper.startSetup(setupFinishedListener);
    }

    public Single<Inventory> getInventory() {
        return mCache;
    }

    public void launchPurchaseFlow(DonateActivity activity, String sku, int requestCode, IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener) throws IabHelper.IabAsyncInProgressException {
        mHelper.launchPurchaseFlow(activity, sku, requestCode, purchaseFinishedListener);
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    public Map<String, Purchase> getPurchaseMap() {
        return mPurchaseMap;
    }

    public void consume(Purchase purchase, IabHelper.OnConsumeFinishedListener listener) throws IabHelper.IabAsyncInProgressException {
        mHelper.consumeAsync(purchase, listener);
    }
}
