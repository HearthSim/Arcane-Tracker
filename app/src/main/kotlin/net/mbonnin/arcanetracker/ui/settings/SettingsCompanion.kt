package net.mbonnin.arcanetracker.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import net.mbonnin.arcanetracker.ui.licenses.LicensesActivity
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsCompanion(internal var settingsView: View) {
    private var firstTime: Boolean = false

    private val mSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            MainViewCompanion.get().alphaSetting = progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }

    fun setTimeoutIndex(index: Int) {
        val quitTimeoutExplanation = settingsView.findViewById<TextView>(R.id.quitTimeout)
        val value = timeouts[index]
        Settings.set(Settings.QUIT_TIMEOUT, index)
        var explanation = settingsView.resources.getString(R.string.quitTimeout)
        if (value >= 0) {
            explanation += settingsView.resources.getString(R.string.minutes, value)
        } else {
            explanation += settingsView.resources.getString(R.string.never)
        }
        quitTimeoutExplanation.text = explanation
    }

    init {
        init()
    }

    private fun bbImageToFile(bbImage: ByteBufferImage): File {
        val now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", Date())
        val file = File(ArcaneTrackerApplication.get().getExternalFilesDir(null), "screenshot_" + now + ".jpg")
        val bitmap = Bitmap.createBitmap(bbImage.w, bbImage.h, Bitmap.Config.ARGB_8888)
        val buffer = bbImage.buffer
        val stride = bbImage.stride
        for (j in 0 until bbImage.h) {
            for (i in 0 until bbImage.w) {
                val r = buffer.get(i * 4 + 0 + j * stride).toInt().and(0xff)
                val g = buffer.get(i * 4 + 1 + j * stride).toInt().and(0xff)
                val b = buffer.get(i * 4 + 2 + j * stride).toInt().and(0xff)
                bitmap.setPixel(i, j, Color.argb(255, r, g, b))
            }
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(file))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return file
    }

    @SuppressLint("NewApi")
    private fun init() {
        val view = settingsView
        val context = ArcaneTrackerApplication.context

        val appVersion = view.findViewById<TextView>(R.id.appVersion)
        appVersion.text = view.context.getString(R.string.thisIsArcaneTracker, BuildConfig.VERSION_NAME, if (Utils.isAppDebuggable) " (debug)" else "")

        val feedbackButton = view.findViewById<Button>(R.id.feedBackButton)
        feedbackButton.setOnClickListener { v ->
            ViewManager.get().removeView(settingsView)

            val arrayUri = ArrayList<Uri>()
            FileTree.get().sync()
            try {
                arrayUri.add(FileProvider.getUriForFile(view.context, "net.mbonnin.arcanetracker.fileprovider", FileTree.get().file))
            } catch (e: Exception) {
                /**
                 * Eat these errors silently. I'm not sure where this comes from and the whole storage APIs might change with Android Q anyways.
                 * This seems to appear only on Huawei Honor anyways (DUK-AL20)
                 *
                 * Fatal Exception: java.lang.IllegalArgumentException: Failed to find configured root that contains /storage/0123-4567/Android/data/net.mbonnin.arcanetracker/files/ArcaneTracker.log
                 * at androidx.core.content.FileProvider$SimplePathStrategy.getUriForFile(FileProvider.java:739)
                 * at androidx.core.content.FileProvider.getUriForFile(FileProvider.java:418)
                 * at net.mbonnin.arcanetracker.ui.settings.SettingsCompanion$1.onClick(SettingsCompanion.kt:90)
                 */
                Utils.reportNonFatal(e)
            }

            val completable: Completable
            val screenCapture = ScreenCaptureHolder.getScreenCapture()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && screenCapture != null) {
                Toast.makeText(view.context, Utils.getString(R.string.preparingEmail), Toast.LENGTH_SHORT).show()

                val fileSingle = Single.create<File> {
                    val imageConsumer = object : ScreenCapture.Consumer {
                        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                        override fun accept(bbImage: ByteBufferImage) {
                            val file = bbImageToFile(bbImage)
                            Timber.d("file1" + file)
                            it.onSuccess(file)
                            screenCapture.removeImageConsumer(this)
                        }
                    }
                    screenCapture.addImageConsumer(imageConsumer)
                }

                Timber.d("file2" )
                /* 1s is hopefully enough for the settings view to disappear  */
                completable = Completable.timer(1, TimeUnit.SECONDS, Schedulers.io())
                        .andThen(fileSingle)
                        .observeOn(AndroidSchedulers.mainThread())
                        .map { file -> arrayUri.add(FileProvider.getUriForFile(view.context, "net.mbonnin.arcanetracker.fileprovider", file)) }
                        .toCompletable()
                Timber.d("file3")
            } else {
                completable = Completable.complete()
            }

            completable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                        emailIntent.type = "text/plain"
                        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@hsreplay.net"))
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arcane Tracker Feedback")
                        emailIntent.putExtra(Intent.EXTRA_TEXT, view.context.getString(R.string.decribeYourProblem) + "\n\n")
                        emailIntent.putExtra(Intent.EXTRA_STREAM, arrayUri)

                        try {
                            ArcaneTrackerApplication.context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            Utils.reportNonFatal(e)
                            Toast.makeText(ArcaneTrackerApplication.context, Utils.getString(R.string.noEmailFound), Toast.LENGTH_LONG).show()
                        }
                    }
        }

        val resetCacheButton = view.findViewById<Button>(R.id.resetCache)
        resetCacheButton.setOnClickListener { v ->
            ArcaneTrackerApplication.get().picassoHddCache.evictAll()
            ArcaneTrackerApplication.get().picassoRamCache.evictAll()
        }

        var seekBar = view.findViewById<SeekBar>(R.id.seekBar)
        seekBar.max = 100
        seekBar.progress = MainViewCompanion.get().alphaSetting
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener)

        val c = MainViewCompanion.get()
        seekBar = view.findViewById<View>(R.id.drawerSizeBar) as SeekBar
        seekBar.max = MainViewCompanion.get().maxDrawerWidth - MainViewCompanion.get().minDrawerWidth
        seekBar.progress = MainViewCompanion.get().drawerWidth - MainViewCompanion.get().minDrawerWidth
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                MainViewCompanion.get().drawerWidth = progress + MainViewCompanion.get().minDrawerWidth
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        val spinner = view.findViewById<View>(R.id.languageSpinner) as Spinner

        val adapter = ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        var selectedPosition = 0
        var i = 1
        val l = Settings.getString(Settings.LANGUAGE, "")
        adapter.add(context.getString(R.string._default))
        for (language in Language.allLanguages) {
            adapter.add(language.friendlyName)
            if (l != "" && language.key == l) {
                selectedPosition = i
            }
            i++
        }

        spinner.adapter = adapter
        spinner.setSelection(selectedPosition)
        firstTime = true
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newKey = if (position == 0) "" else Language.allLanguages[position - 1].key
                val oldKey = Settings.getString(Settings.LANGUAGE, "")
                if (!firstTime && newKey != oldKey) {
                    Settings.set(Settings.LANGUAGE, newKey)

                    showRestartDialog()
                }
                firstTime = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        seekBar = view.findViewById<View>(R.id.buttonSizeBar) as SeekBar
        seekBar.max = c.maxButtonWidth - c.minButtonWidth
        seekBar.progress = c.buttonWidth - c.minButtonWidth
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                c.buttonWidth = progress + c.minButtonWidth
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        val screenCapture = view.findViewById<View>(R.id.screenCaptureCheckBox) as CheckBox
        screenCapture.isChecked = Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true)
        screenCapture.setOnCheckedChangeListener { buttonView, isChecked ->
            Settings.set(Settings.SCREEN_CAPTURE_ENABLED, isChecked)
        }

        val turnTimer = view.findViewById<View>(R.id.turnTimerCheckBox) as CheckBox
        turnTimer.isChecked = Settings.get(Settings.TURN_TIMER_ENABLED, true)
        turnTimer.setOnCheckedChangeListener { buttonView, isChecked ->
            Settings.set(Settings.TURN_TIMER_ENABLED, isChecked)
        }

        val autoHide = view.findViewById<View>(R.id.autoHideCheckBox) as CheckBox
        autoHide.isChecked = Settings.get(Settings.AUTO_HIDE, false)
        autoHide.setOnCheckedChangeListener { _, isChecked ->
            Settings.set(Settings.AUTO_HIDE, isChecked)
        }

        val quitTimeoutSeekbar = view.findViewById<SeekBar>(R.id.quitTimeoutSeekbar)
        quitTimeoutSeekbar.max = timeouts.size - 1
        setTimeoutIndex(getTimeoutIndex())
        quitTimeoutSeekbar.progress = getTimeoutIndex()
        quitTimeoutSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setTimeoutIndex(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        val hsreplayDescription = view.findViewById<TextView>(R.id.hsReplayDescription)
        val hsReplay = ArcaneTrackerApplication.get().hsReplay
        val account = hsReplay.account()
        val enabled = if (account != null) context.getString(R.string.enabled) else context.getString(R.string.disabled)
        hsreplayDescription.setText(context.getString(R.string.hsreplayDescription, enabled, account?.username))
        val licensesButton = view.findViewById<Button>(R.id.licenses)
        licensesButton.setOnClickListener {
            ViewManager.get().removeView(settingsView)

            val intent = Intent()
            intent.setClass(context, LicensesActivity::class.java)
            context.startActivity(intent)
        }

    }

    private fun showRestartDialog() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !android.provider.Settings.canDrawOverlays(settingsView.context)) {
            /*
             * We come here if we open the settings from the notification
             * In that case, we might not have the overlay permissions.
             * We won't show the restart dialog but that's most likely ok as
             * the user will have to enable overlay permissions before seeing a card anyways
             */
            return
        }

        val view2 = LayoutInflater.from(settingsView.context).inflate(R.layout.please_restart, null)
        view2.findViewById<View>(R.id.ok).setOnClickListener { ViewManager.get().removeView(view2) }

        val params = ViewManager.Params()
        params.w = (ViewManager.get().width * 0.6f).toInt()
        params.h = ViewManager.get().height / 2
        params.x = (ViewManager.get().width - params.w) / 2
        params.y = ViewManager.get().height / 4

        ViewManager.get().addModalView(view2, params)
    }

    companion object {
        val timeouts = arrayOf(3, 5, 10, 30, 60, -1)

        fun getTimeoutIndex(): Int {
            return Settings.get(Settings.QUIT_TIMEOUT, 1)
        }

        fun getTimeoutValue(): Int {
            val index = getTimeoutIndex()
            if (index < 0) {
                return index
            } else {
                return timeouts[index]
            }
        }


        fun show() {
            val context = ArcaneTrackerApplication.context
            val viewManager = ViewManager.get()
            val view2 = LayoutInflater.from(context).inflate(R.layout.settings_view, null)

            SettingsCompanion(view2)

            val params = ViewManager.Params()
            params.x = (viewManager.width * 0.15).toInt()
            params.y = viewManager.height / 16
            params.w = (viewManager.width * 0.70).toInt()
            params.h = 7 * viewManager.height / 8

            viewManager.addModalAndFocusableView(view2, params)
        }
    }
}
