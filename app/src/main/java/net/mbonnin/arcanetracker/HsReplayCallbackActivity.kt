package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Bundle
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.hs_replay_callback_activity.*
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import timber.log.Timber

class HsReplayCallbackActivity : Activity() {
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.hs_replay_callback_activity)

        textView.setText(R.string.hsReplayClaiming)
        button.setVisible(false)

        button.setText(R.string.backToGame)
        button.setOnClickListener {
            finishAndRemoveTaskIfPossible()
            val hsIntent = packageManager.getLaunchIntentForPackage(MainActivity.HEARTHSTONE_PACKAGE_ID)
            if (hsIntent != null) {
                startActivity(hsIntent)
            }
        }

        val code = intent.data.getQueryParameter("code")

        if (code.isNullOrEmpty()) {
            finishAndRemoveTaskIfPossible()
        }

        disposable = Completable.fromCallable {
            OauthInterceptor.exchangeCode(code)
        }.andThen(HSReplay.get().claimTokenOauth())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    progressBar.setVisible(false)
                    button.setVisible(true)
                    textView.setText(R.string.hsReplayClaimSuccess)
                }, {
                    Timber.e(it)
                    progressBar.setVisible(false)
                    button.setVisible(true)
                    textView.setText(R.string.hsReplayClaimFailed)
                })

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }
}
