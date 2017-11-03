package net.mbonnin.arcanetracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.crash.FirebaseCrash
import rx.Completable
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


object Utils {

    val ISO8601DATEFORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

    val is7InchesOrHigher: Boolean
        get() {
            val context = ArcaneTrackerApplication.getContext()
            val display = (context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val xdpi = context.resources.displayMetrics.xdpi
            val ydpi = context.resources.displayMetrics.ydpi
            val xinches = outMetrics.heightPixels / xdpi
            val yinches = outMetrics.widthPixels / ydpi

            return Math.hypot(xinches.toDouble(), yinches.toDouble()) > 7
        }

    val isAppDebuggable: Boolean
        get() {
            val context = ArcaneTrackerApplication.getContext()
            return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }

    val hsExternalDir: String
        get() = Environment.getExternalStorageDirectory().path + "/Android/data/com.blizzard.wtcg.hearthstone/files/"

    val isNetworkConnected: Boolean
        get() {
            val cm = ArcaneTrackerApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                ArcaneTrackerApplication.getContext().resources.displayMetrics).toInt()
    }

    fun getDrawableForName(name: String): Drawable {
        val context = ArcaneTrackerApplication.getContext()
        val id = context.resources.getIdentifier(name.toLowerCase(), "drawable", context.packageName)
        if (id > 0) {
            return context.resources.getDrawable(id)
        } else {
            Timber.e("could not find a bar for id " + name)
            return context.resources.getDrawable(R.drawable.hero_10)
        }
    }

    fun getDrawableForClassIndex(classIndex: Int): Drawable {
        return getDrawableForName(getHeroId(classIndex))
    }

    fun logWithDate(s: String) {
        val c = Calendar.getInstance()

        val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss ", Locale.ENGLISH)

        Timber.w(df.format(c.time) + s)
    }

    fun reportNonFatal(e: Throwable) {
        Timber.w(e)
        FirebaseCrash.report(e)
    }

    fun cardMapTotal(map: HashMap<String, Int>): Int {
        var total = 0
        for (key in map.keys) {
            total += cardMapGet(map, key)
        }

        return total
    }

    fun getAssetBitmap(name: String): Bitmap? {
        val context = ArcaneTrackerApplication.getContext()
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(name)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return BitmapFactory.decodeStream(inputStream)
    }

    fun getAssetBitmap(name: String, defaultName: String): Bitmap? {
        val b = getAssetBitmap(name)
        return b ?: getAssetBitmap(defaultName)
    }

    fun getCardUrl(id: String): String {
        return "card://" + Language.getCurrentLanguage().key + "/" + id
    }

    fun exitApp() {
        Overlay.get().hide()
        FileTree.get().sync()
        MainService.stop()
        System.exit(0)
    }

    fun openLink(url: String) {
        val i = Intent()
        i.action = Intent.ACTION_VIEW
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.data = Uri.parse(url)
        try {
            ArcaneTrackerApplication.getContext().startActivity(i)
        } catch (e: Exception) {
            Utils.reportNonFatal(e)
            Toast.makeText(ArcaneTrackerApplication.getContext(), Utils.getString(R.string.noBrowserFound), Toast.LENGTH_LONG).show()
        }

    }

    fun valueOf(i: Int?): Int {
        return i ?: 0
    }

    fun getString(resId: Int, vararg args: Any): String {
        return ArcaneTrackerApplication.getContext().getString(resId, *args)
    }

    fun isEmpty(str: String?): Boolean {
        if (str == null) {
            return true
        }

        return "" == str
    }

    fun equalsNullSafe(a: String?, b: String?): Boolean {
        return a == b
    }

    fun cardMapGet(map: HashMap<String, Int>, key: String): Int {
        var a: Int? = map[key]
        if (a == null) {
            a = 0
        }
        return a
    }

    fun cardMapAdd(map: HashMap<String, Int>, key: String, diff: Int) {
        val a = cardMapGet(map, key)
        map.put(key, a + diff)
    }

    fun runOnMainThread(action: () -> Unit) {
        Completable.fromAction(action)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
}

