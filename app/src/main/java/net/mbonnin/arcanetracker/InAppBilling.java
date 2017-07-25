package net.mbonnin.arcanetracker;

import android.content.Context;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;

import java.util.ArrayList;

import rx.Single;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class InAppBilling {
    private BehaviorSubject<Inventory> mBehaviourSubject = null;
    private IabHelper mHelper;
    private static final String TAG = "InAppBilling";
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

    private IabHelper.QueryInventoryFinishedListener inventoryListener = (result, inv) -> {
        if (result.isFailure()) {
            mBehaviourSubject.onError(new Exception(result + ""));
        } else {
            mBehaviourSubject.onNext(inv);
            mBehaviourSubject.onCompleted();
        }
    };
    IabHelper.OnIabSetupFinishedListener setupFinishedListener = result -> {
        if (!result.isSuccess()) {
            // Oh no, there was a problem.
            Timber.d(TAG, "Problem setting up In-app Billing: " + result);
            mBehaviourSubject.onError(new Exception(result + ""));
        } else {
            Timber.d(TAG, "Hooray, IAB is fully set up ");

            ArrayList<String> skuList = new ArrayList<>();
            skuList.add(SKU_HOLY_LIGHT);
            skuList.add(SKU_BLESSING_OF_KINGS);
            skuList.add(SKU_DINOSIZE);
            try {
                mHelper.queryInventoryAsync(true, skuList, null, inventoryListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
                mBehaviourSubject.onError(new Exception());
            }
        }
    };

    public InAppBilling(Context context) {
        mBehaviourSubject = BehaviorSubject.create();

        mHelper = new IabHelper(context, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoYhDA6fIn/7GLTass2NT6fWYGP6ZZ+nBxEdn5zMY+fgGfthup2R8J5N8Is/nKke0GujqWyLUafyPiutGHMARGTkaTo2PmedBFMMMMShAyYVciP7V7lByk8leqKbwWjoSw69YzYeeIJUqeEB/PALLf05Gn8jRAXwwknb2+GgIc7sW9B/QzcwJuj21mk6TAAmNZhzcrIe4S3hi73+VtmXToQaHJMb464JWq5xScyI+NIhpFLMgZotsBwI8sD1nnQnAFrLSyjUU0891dz0nTbzdTb1FacwFTZM+qpqVSf5rnI6nS22dMkOne2q9C0336COUGEIVhwIJCxN6O0EbQ22ZpwIDAQAB");
        mHelper.startSetup(setupFinishedListener);
    }

    public IabHelper getHelper() {
        return mHelper;
    }

    public Single<Inventory> getInventory() {
        return mBehaviourSubject.toSingle();
    }
}
