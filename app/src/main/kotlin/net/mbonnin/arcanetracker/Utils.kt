package net.mbonnin.arcanetracker

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import net.mbonnin.arcanetracker.ui.overlay.Overlay
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object Utils {

    val ISO8601DATEFORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

    val is7InchesOrHigher: Boolean
        get() {
            val context = ArcaneTrackerApplication.context
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
            val context = ArcaneTrackerApplication.context
            return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                ArcaneTrackerApplication.context.resources.displayMetrics).toInt()
    }

    fun getDrawableForNameDeprecated(name: String): Drawable {
        val context = ArcaneTrackerApplication.context
        val id = context.resources.getIdentifier(name.toLowerCase(), "drawable", context.packageName)
        if (id > 0) {
            return context.resources.getDrawable(id)
        } else {
            Timber.e("could not find a bar for id " + name)
            return context.resources.getDrawable(R.color.black)
        }
    }

    fun getDrawableForName(name: String): Drawable? {
        val context = ArcaneTrackerApplication.context
        val id = context.resources.getIdentifier(name.toLowerCase(), "drawable", context.packageName)
        if (id > 0) {
            return context.resources.getDrawable(id)
        } else {
            Timber.e("could not find a drawable for name " + name)
            return null
        }
    }

    fun logWithDate(s: String) {
        val c = Calendar.getInstance()

        val df = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss ", Locale.ENGLISH)

        Timber.w(df.format(c.time) + s)
    }

    fun reportNonFatal(e: Throwable) {
        Timber.w(e)
        FirebaseCrashlytics.getInstance().recordException(e);
    }


    fun getAssetBitmap(name: String): Bitmap? {
        val context = ArcaneTrackerApplication.context
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(name)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return BitmapFactory.decodeStream(inputStream)
    }

    fun getCardUrl(id: String): String {
        // On my Pixel 3XL, a card is ~500px wide. Putting 256x down there will result in significant blur
        // It would be nice to have webp or jpg on the server but they're not present as of today
        return "https://art.hearthstonejson.com/v1/render/latest/${Language.currentLanguage.jsonName}/512x/${id}.png"
    }

    fun getTileUrl(id: String): String {
        return "https://art.hearthstonejson.com/v1/tiles/${id}.png"
    }
    fun exitApp() {
        Overlay.hide()
        FileTree.get().sync()
        MainService.stop()

        /**
         * Leave some time to the OS to acknowledge the fact that the service is stopped and to prevent it
         * from restarting it
         */
        Handler().postDelayed( {
            System.exit(0)
        }, 1000)
    }

    fun openLink(url: String) {
        val i = Intent()
        i.action = Intent.ACTION_VIEW
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.data = Uri.parse(url)
        try {
            ArcaneTrackerApplication.context.startActivity(i)
        } catch (e: Exception) {
            Utils.reportNonFatal(e)
            Toast.makeText(ArcaneTrackerApplication.context, Utils.getString(R.string.noBrowserFound), Toast.LENGTH_LONG).show()
        }

    }

    fun valueOf(i: Int?): Int {
        return i ?: 0
    }

    fun getString(resId: Int, vararg args: Any): String {
        return ArcaneTrackerApplication.context.getString(resId, *args)
    }

    fun isEmpty(str: String?): Boolean {
        if (str == null) {
            return true
        }

        return "" == str
    }

    fun runOnMainThread(action: () -> Unit) {
        Completable.fromAction(action)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }
}

