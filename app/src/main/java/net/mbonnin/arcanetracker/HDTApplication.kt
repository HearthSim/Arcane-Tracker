package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import androidx.multidex.MultiDexApplication
import com.google.firebase.analytics.FirebaseAnalytics
import com.jakewharton.picasso.OkHttp3Downloader
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import io.paperdb.Paper
import net.hearthsim.kotlin.hslog.PowerParser
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.parser.*
import net.mbonnin.arcanetracker.trackobot.Trackobot
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.enum.PlayerClass
import okhttp3.Cache
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.util.*

class HDTApplication : MultiDexApplication() {
    lateinit var picassoRamCache: LruCache
    lateinit var picassoHddCache: Cache

    var hearthstoneBuild = 0

    private fun defaultCacheDir(): File {
        val cache = File(cacheDir, "picasso_cache")
        if (!cache.exists()) {

            cache.mkdirs()
        }
        return cache
    }

    @SuppressLint("NewApi", "CheckResult")
    override fun onCreate() {
        super.onCreate()

        sHDTApplication = this
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
        if (oldVersion in 1..399
                && BuildConfig.VERSION_CODE >= 400) {
            Settings.set(Settings.IS_PRE_HEARTHSIM_USER, true)
        }
        if (oldVersion in 1..401
                && BuildConfig.VERSION_CODE >= 402
                && Trackobot.get().currentUser() != null) {
            Settings.set(Settings.IS_LEGACY_TRACKOBOT_USER, true)
        }
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
        Timber.d("Build.MODEL=\${Build.MODEL}")
        Timber.d("Build.MANUFACTURER=${Build.MANUFACTURER}")
        Timber.d("screen size= ${point.x} x ${point.y}")
        Timber.d("sizeInInches=${sizeInInches}")

        Utils.logWithDate("HDTApplication.onCreate() + version=" + BuildConfig.VERSION_CODE)

        Paper.init(this)

        picassoRamCache = LruCache(this)

        picassoHddCache = Cache(defaultCacheDir(), 200 * 1024 * 1024)
        val okHttpClient = OkHttpClient.Builder()
                .cache(picassoHddCache)
                .build()
        val downloader = OkHttp3Downloader(okHttpClient)

        val picasso = Picasso.Builder(this)
                .memoryCache(picassoRamCache!!)
                .downloader(downloader)
                .build()

        Picasso.setSingletonInstance(picasso)
        if (Utils.isAppDebuggable) {
            Picasso.with(this).setIndicatorsEnabled(true)
        }

        StopServiceBroadcastReceiver.init()

        QuitDetector.get().start()
        ScreenCaptureHolder.start()

        initCardJson()

        /*
         * we need to read the whole loading screen if we start the Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        val loadingScreenLogReader = LogReader("LoadingScreen.log", false)
        loadingScreenLogReader.start(LoadingScreenParser.get())


        GameLogic.get().addListener(GameLogicListener.get())
        val handler = Handler()
        val powerParser = PowerParser({ tag ->
            handler.post { GameLogic.get().handleRootTag(tag) }
        }, { gameStr, gameStart ->
            GameLogicListener.get().uploadGame(gameStr, gameStart)
        }, { format, args ->
            Timber.d(format, *args)
        })
        /*
         * Power.log, we just want the incremental changes
         */
        val powerLogReader = LogReader("Power.log", true)
        powerLogReader.start(object : LogReader.LineConsumer {
            var previousDataRead = false
            override fun onLine(rawLine: String) {
                powerParser.process(rawLine, previousDataRead)
            }

            override fun onPreviousDataRead() {
                previousDataRead = true
            }
        })


        val decksLogReader = LogReader("Decks.log", false)
        decksLogReader.start(DecksParser.get())

        val achievementLogReader = LogReader("Achievements.log", true)
        achievementLogReader.start(AchievementsParser())

        HSReplay.userAgent = (HDTApplication.context.getPackageName() + "/" + BuildConfig.VERSION_NAME
                + "; Android " + Build.VERSION.RELEASE + ";")
        HSReplay.context = this
        if (HSReplay.get().token() == null) {
            HSReplay.get().createToken().subscribe()
        }

        MainService.start()

        FirebaseAnalytics.getInstance(this).setUserProperty(FirebaseConstants.SCREEN_CAPTURE_ENABLED.name.toLowerCase(),
                Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true).toString())
    }

    private fun initCardJson() {
        val jsonName = Language.currentLanguage.jsonName

        val injectedCards = ArrayList<Card>()

        /*
         * these are 3 fake cards needed for CardRender
         */
        injectedCards.add(CardUtil.secret(PlayerClass.PALADIN))
        injectedCards.add(CardUtil.secret(PlayerClass.HUNTER))
        injectedCards.add(CardUtil.secret(PlayerClass.MAGE))
        injectedCards.add(CardUtil.secret(PlayerClass.ROGUE))

        CardJson.init(jsonName, injectedCards)
    }

    companion object {
        val NOTIFICATION_CHANNEL_ID = "channel_id"
        lateinit var context: Context
            private set

        private lateinit var sHDTApplication: HDTApplication

        fun get(): HDTApplication {
            return sHDTApplication
        }
    }

}
