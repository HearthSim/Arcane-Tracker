package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import androidx.multidex.MultiDexApplication
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import io.paperdb.Paper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.streams.asInput
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hslog.HSLog
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.PlayerClass
import net.hearthsim.hsreplay.HsReplay
import okhttp3.Cache
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.*

class ArcaneTrackerApplication : MultiDexApplication() {
    lateinit var hsReplay: HsReplay
        private set
    lateinit var picassoRamCache: LruCache
        private set
    lateinit var picassoHddCache: Cache
        private set
    lateinit var cardJson: CardJson
        private set
    lateinit var hsLog: HSLog
        private set
    lateinit var analytics: Analytics
        private set

    var hearthstoneBuild = 0

    val console = object : Console {
        override fun debug(message: String) {
            Timber.d(message)
        }

        override fun error(message: String) {
            Timber.e(message)
        }

        override fun error(throwable: Throwable) {
            Timber.e(throwable)
        }
    }

    private fun defaultCacheDir(): File {
        val cache = File(cacheDir, "picasso_cache")
        if (!cache.exists()) {

            cache.mkdirs()
        }
        return cache
    }

    private fun createCardJson(): CardJson {
        val jsonName = Language.currentLanguage.jsonName

        val injectedCards = ArrayList<Card>()

        /*
         * these are fake cards needed for CardRender
         */
        injectedCards.add(CardUtil.secret(PlayerClass.PALADIN))
        injectedCards.add(CardUtil.secret(PlayerClass.HUNTER))
        injectedCards.add(CardUtil.secret(PlayerClass.MAGE))
        injectedCards.add(CardUtil.secret(PlayerClass.ROGUE))

        val input = resources.openRawResource(R.raw.cards).asInput()

        return CardJson(jsonName, injectedCards, input)
    }


    @SuppressLint("NewApi", "CheckResult")
    override fun onCreate() {
        super.onCreate()

        /*
         * There is a small race condition here if the user tries to login too early but installing
         * the provider should be fast enough that it's not noticeable
         */
        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstallFailed(p0: Int, p1: Intent?) {
                Utils.reportNonFatal(Exception("cannot install latest security provider"))
            }

            override fun onProviderInstalled() {
                Timber.d("new provider installed")
            }
        })


        sArcaneTrackerApplication = this
        context = object : ContextThemeWrapper(this, R.style.AppTheme) {
            override fun startActivity(intent: Intent) {
                if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0) {
                    /*
                     * XXX: this is a hack to be able to click textview links
                     */
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                super.startActivity(intent)
            }
        }

        val oldVersion = Settings[Settings.VERSION, 0]
        Timber.plant(FileTree.get())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = getString(R.string.app_name)
            val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mChannel.setShowBadge(false)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val point = Point()
        display.getRealSize(point)

        val sizeInInches = Math.sqrt((point.x * point.x / (metrics.xdpi * metrics.xdpi) + point.y * point.y / (metrics.ydpi * metrics.ydpi)).toDouble())
        Timber.d("Build.MODEL=${Build.MODEL}")
        Timber.d("Build.MANUFACTURER=${Build.MANUFACTURER}")
        Timber.d("screen size= ${point.x} x ${point.y}")
        Timber.d("sizeInInches=${sizeInInches}")

        Utils.logWithDate("ArcaneTrackerApplication.onCreate() + version=" + BuildConfig.VERSION_CODE)

        Paper.init(this)

        picassoRamCache = LruCache(this)

        picassoHddCache = Cache(defaultCacheDir(), 200 * 1024 * 1024)
        val okHttpClient = OkHttpClient.Builder()
                .cache(picassoHddCache)
                .build()
        val downloader = OkHttp3Downloader(okHttpClient)

        val picasso = Picasso.Builder(this)
                .memoryCache(picassoRamCache)
                .downloader(downloader)
                .build()

        Picasso.setSingletonInstance(picasso)
        if (Utils.isAppDebuggable) {
            Picasso.with(this).setIndicatorsEnabled(true)
        }

        StopServiceBroadcastReceiver.init()

        HideDetector.get().start()
        ScreenCaptureHolder.start()

        cardJson = createCardJson()
        analytics = ArcaneTrackerAnalytics()
        hsLog = HSLogFactory.createHSLog(console, cardJson)

        val userAgent = (ArcaneTrackerApplication.context.packageName + "/" + BuildConfig.VERSION_NAME
                + "; Android " + Build.VERSION.RELEASE + ";")

        hsReplay = HsReplay(HsReplayPreferences(this), console, analytics, userAgent)

        MainService.start()

        val account = hsReplay.account()
        if (account != null) {

            FirebaseAnalytics.getInstance(this).setUserProperty(FirebaseConstants.IS_PREMIUM.name.toLowerCase(),
                    account.is_premium.toString())

            if (hsReplay.hasValidAccessToken()) {
                GlobalScope.launch {
                    // Update the name in the background. It's not the end of the world if the status is wrong during a few seconds
                    hsReplay.refreshAccountInformation()
                }
            }
        }
        FirebaseAnalytics.getInstance(this).setUserProperty(FirebaseConstants.IS_LEGACY.name.toLowerCase(),
                Settings.get(Settings.IS_PRE_HEARTHSIM_USER, false).toString())
        FirebaseAnalytics.getInstance(this).setUserProperty(FirebaseConstants.SCREEN_CAPTURE_ENABLED.name.toLowerCase(),
                Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true).toString())
    }

    companion object {
        val NOTIFICATION_CHANNEL_ID = "channel_id"
        lateinit var context: Context
            private set

        private lateinit var sArcaneTrackerApplication: ArcaneTrackerApplication

        fun get(): ArcaneTrackerApplication {
            return sArcaneTrackerApplication
        }
    }

}
