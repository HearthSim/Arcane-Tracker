package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.FileProvider
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import com.google.firebase.analytics.FirebaseAnalytics
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import net.mbonnin.arcanetracker.hsreplay.model.Lce
import net.mbonnin.arcanetracker.hsreplay.model.Token
import net.mbonnin.arcanetracker.trackobot.Trackobot
import net.mbonnin.arcanetracker.trackobot.Url
import net.mbonnin.arcanetracker.trackobot.User
import rx.Completable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsCompanion(internal var settingsView: View) {
    private var mHsReplayCompanion1: LoadableButtonCompanion? = null
    private var trackobotText: TextView? = null
    private var signinButton: Button? = null
    private var signupButton: Button? = null
    private var usernameEditText: EditText? = null
    private var passwordEditText: EditText? = null
    private var signupProgressBar: ProgressBar? = null
    private var signinProgressBar: ProgressBar? = null
    private var retrievePassword: View? = null
    private var importButton: Button? = null
    private var importExplanation: View? = null
    private var importProgressBar: ProgressBar? = null
    private var firstTime: Boolean = false
    private var mHsReplayCompanion2: LoadableButtonCompanion? = null

    private val mSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            MainViewCompanion.get().alphaSetting = progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }

    private val mSigninButtonClicked = View.OnClickListener {
        val user = User(usernameEditText!!.text.toString(), passwordEditText!!.text.toString())

        Trackobot.get().testUser(user)
                .subscribe { lce ->
                    if (lce.isLoading) {
                        signinButton!!.visibility = GONE
                        signinProgressBar!!.visibility = VISIBLE
                        signupButton!!.isEnabled = false
                    } else {
                        signinProgressBar!!.visibility = GONE
                        signinButton!!.visibility = VISIBLE
                        signupButton!!.isEnabled = true
                        if (lce.error != null) {
                            Toast.makeText(ArcaneTrackerApplication.context, ArcaneTrackerApplication.context.getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show()
                            Utils.reportNonFatal(lce.error)
                        } else {
                            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("track_o_bot_signin", null)
                            updateTrackobot(settingsView)
                        }
                    }
                }
    }

    private val mSignupButtonClicked = View.OnClickListener {
        Trackobot.get().createUser()
                .subscribe { lce ->
                    if (lce.isLoading) {
                        signupButton!!.visibility = GONE
                        signupProgressBar!!.visibility = VISIBLE
                        signinButton!!.isEnabled = false
                    } else {
                        signupProgressBar!!.visibility = GONE
                        signupButton!!.visibility = VISIBLE
                        signinButton!!.isEnabled = true

                        val context = ArcaneTrackerApplication.context
                        if (lce.error != null) {
                            Toast.makeText(context, context.getString(R.string.trackobotSignupError), Toast.LENGTH_LONG).show()
                            Utils.reportNonFatal(lce.error)
                        } else {
                            Trackobot.get().link(lce.data)

                            FirebaseAnalytics.getInstance(context).logEvent("track_o_bot_signup", null)
                            updateTrackobot(settingsView)
                        }
                    }
                }
    }

    private fun onTrackobotUrl(url: Url) {
        ViewManager.Companion.get().removeView(settingsView)

        Utils.openLink(url.url)
    }

    private fun onTrackobotUrlError(throwable: Throwable) {
        val context = ArcaneTrackerApplication.context
        Toast.makeText(context, context.getString(R.string.couldNotGetProfile), Toast.LENGTH_LONG).show()
        signupButton!!.visibility = VISIBLE
        signupProgressBar!!.visibility = GONE
        Timber.e(throwable)
    }

    private val mImportButtonClicked = View.OnClickListener {
        val context = ArcaneTrackerApplication.context
        val f = Trackobot.findTrackobotFile()
        if (f == null) {
            Toast.makeText(context, context.getString(R.string.couldNotFindTrackobotFile), Toast.LENGTH_LONG).show()
            return@OnClickListener
        }

        val user = Trackobot.parseTrackobotFile(f)
        if (user == null) {
            Toast.makeText(context, context.getString(R.string.couldNotOpenTrackobotFile), Toast.LENGTH_LONG).show()
            return@OnClickListener
        }

        Trackobot.get().testUser(user)
                .subscribe { lce ->
                    if (lce.isLoading) {
                        importButton!!.visibility = GONE
                        importProgressBar!!.visibility = VISIBLE
                        importButton!!.isEnabled = false
                    } else {
                        importProgressBar!!.visibility = GONE
                        importButton!!.visibility = VISIBLE
                        importButton!!.isEnabled = true

                        if (lce.error != null) {
                            Toast.makeText(ArcaneTrackerApplication.context, ArcaneTrackerApplication.context.getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show()
                            Utils.reportNonFatal(lce.error)
                        } else {
                            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("track_o_bot_import", null)
                            updateTrackobot(settingsView)
                        }
                    }
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

    internal var mHsReplayState = HsReplayState()


    private fun updateTrackobot(view: View) {
        signupButton = view.findViewById<View>(R.id.trackobotSignup) as Button
        signinButton = view.findViewById<View>(R.id.trackobotSignin) as Button
        trackobotText = view.findViewById<View>(R.id.trackobotText) as TextView
        passwordEditText = view.findViewById<View>(R.id.password) as EditText
        usernameEditText = view.findViewById<View>(R.id.username) as EditText
        signinProgressBar = view.findViewById<View>(R.id.signinProgressBar) as ProgressBar
        signupProgressBar = view.findViewById<View>(R.id.signupProgressBar) as ProgressBar
        retrievePassword = view.findViewById(R.id.retrievePassword)
        importButton = view.findViewById<View>(R.id.trackobotImport) as Button
        importProgressBar = view.findViewById<View>(R.id.importProgressBar) as ProgressBar
        importExplanation = view.findViewById(R.id.importExplanation)

        val user = Trackobot.get().currentUser()
        if (user == null) {
            trackobotText!!.text = view.context.getString(R.string.trackobotExplanation)
            view.findViewById<View>(R.id.or).visibility = VISIBLE

            usernameEditText!!.isEnabled = true
            passwordEditText!!.isEnabled = true

            signinButton!!.text = view.context.getString(R.string.linkAccount)
            signinButton!!.setOnClickListener(mSigninButtonClicked)

            signupButton!!.text = view.context.getString(R.string.createAccount)
            signupButton!!.setOnClickListener(mSignupButtonClicked)

            retrievePassword!!.visibility = VISIBLE

            importButton!!.text = view.context.getString(R.string.importFromStorage)
            importButton!!.setOnClickListener(mImportButtonClicked)
            importButton!!.isEnabled = true
            importButton!!.visibility = VISIBLE
            view.findViewById<View>(R.id.or2).visibility = VISIBLE
            importExplanation!!.visibility = VISIBLE


        } else {
            trackobotText!!.visibility = GONE
            view.findViewById<View>(R.id.or).visibility = GONE

            usernameEditText!!.setText(user.username)
            passwordEditText!!.setText(user.password)
            usernameEditText!!.isEnabled = false
            passwordEditText!!.isEnabled = false

            signinButton!!.text = view.context.getString(R.string.unlinkAccount)
            signinButton!!.setOnClickListener { v ->
                Trackobot.get().unlink()
                usernameEditText!!.setText("")
                passwordEditText!!.setText("")
                updateTrackobot(settingsView)
            }

            signupButton!!.text = view.context.getString(R.string.openInBrowser)
            signupButton!!.setOnClickListener { v ->
                signupProgressBar!!.visibility = VISIBLE
                signupButton!!.visibility = GONE

                Trackobot.get().createOneTimeAuth()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTrackobotUrl, this::onTrackobotUrlError)
            }

            retrievePassword!!.visibility = GONE

            importExplanation!!.visibility = GONE
            importButton!!.visibility = GONE
            view.findViewById<View>(R.id.or2).visibility = GONE

        }
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

        updateTrackobot(view)

        val appVersion = view.findViewById<TextView>(R.id.appVersion)
        appVersion.text = view.context.getString(R.string.thisIsArcaneTracker, BuildConfig.VERSION_NAME, if (Utils.isAppDebuggable) " (debug)" else "")

        val feedbackButton = view.findViewById<Button>(R.id.feedBackButton)
        feedbackButton.setOnClickListener { v ->
            ViewManager.Companion.get().removeView(settingsView)

            val arrayUri = ArrayList<Uri>()
            FileTree.get().sync()
            arrayUri.add(FileProvider.getUriForFile(view.context, "net.mbonnin.arcanetracker.fileprovider", FileTree.get().file))

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
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@arcanetracker.com"))
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
        resetCacheButton.setOnClickListener { v -> PicassoCardRequestHandler.get().resetCache() }

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

        val autoSelectDeck = view.findViewById<View>(R.id.autoSelectDeck) as CheckBox
        autoSelectDeck.isChecked = Settings.get(Settings.AUTO_SELECT_DECK, true)
        autoSelectDeck.setOnCheckedChangeListener { buttonView, isChecked -> Settings.set(Settings.AUTO_SELECT_DECK, isChecked) }

        val autoAddCards = view.findViewById<View>(R.id.autoAddCards) as CheckBox
        autoAddCards.isChecked = Settings.get(Settings.AUTO_ADD_CARDS, true)
        autoAddCards.setOnCheckedChangeListener { buttonView, isChecked -> Settings.set(Settings.AUTO_ADD_CARDS, isChecked) }

        val showLegacyDecks = view.findViewById<View>(R.id.legacyDecks) as CheckBox
        showLegacyDecks.isChecked = Settings.get(Settings.SHOW_LEGACY_DECKS, false)
        showLegacyDecks.setOnCheckedChangeListener { buttonView, isChecked -> c.handlesView.showLegacy(isChecked) }

        val hsReplay1 = view.findViewById<View>(R.id.hsReplayButton1)
        val hsReplay2 = view.findViewById<View>(R.id.hsReplayButton2)

        mHsReplayCompanion1 = LoadableButtonCompanion(hsReplay1)
        mHsReplayCompanion2 = LoadableButtonCompanion(hsReplay2)

        mHsReplayState.token = HSReplay.get().token()
        if (mHsReplayState.token != null) {
            checkUserName()
        }
        updateHsReplay()

        val licensesButton = view.findViewById<Button>(R.id.licenses)
        licensesButton.setOnClickListener { v ->
            ViewManager.Companion.get().removeView(settingsView)

            val intent = Intent()
            intent.setClass(context, LicensesActivity::class.java)
            context.startActivity(intent)
        }

    }

    private fun showRestartDialog() {
        val view2 = LayoutInflater.from(settingsView.context).inflate(R.layout.please_restart, null)
        view2.findViewById<View>(R.id.ok).setOnClickListener { v3 -> ViewManager.Companion.get().removeView(view2) }

        val params = ViewManager.Params()
        params.w = (ViewManager.Companion.get().width * 0.6f).toInt()
        params.h = ViewManager.Companion.get().height / 2
        params.x = (ViewManager.Companion.get().width - params.w) / 2
        params.y = ViewManager.Companion.get().height / 4

        ViewManager.Companion.get().addModalView(view2, params)
    }

    private fun checkUserName() {
        HSReplay.get().user()
                .subscribe { handleUserLce(it)}
    }

    internal class HsReplayState {
        var token: String? = null
        var tokenLoading: Boolean = false
        var userName: String? = null
        var userNameLoading: Boolean = false
        var claimUrlLoading: Boolean = false
    }

    private fun handleUserLce(lce: Lce<Token>) {
        if (lce.isLoading) {
            mHsReplayState.userNameLoading = true
        } else if (lce.data != null) {
            mHsReplayState.userNameLoading = false
            if (lce.data.user != null && lce.data.user!!.username != null) {
                mHsReplayState.userName = lce.data.user!!.username
            }
        } else if (lce.error != null) {
            mHsReplayState.userNameLoading = false
            Utils.reportNonFatal(Exception("HsReplay username error", lce.error))
            Toast.makeText(settingsView.context, Utils.getString(R.string.hsReplayUsernameError), Toast.LENGTH_LONG).show()
        }
        updateHsReplay()
    }

    private fun handleTokenLce(lce: net.mbonnin.arcanetracker.hsreplay.model.Lce<String>) {
        if (lce.isLoading) {
            mHsReplayState.tokenLoading = true
        } else if (lce.error != null) {
            mHsReplayState.tokenLoading = false
            Utils.reportNonFatal(Exception("HsReplay token error", lce.error))
            Toast.makeText(settingsView.context, Utils.getString(R.string.hsReplayTokenError), Toast.LENGTH_LONG).show()
        } else {
            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("hsreplay_enable", null)

            mHsReplayState.tokenLoading = false
            mHsReplayState.token = lce.data
        }
        updateHsReplay()

    }

    private fun updateHsReplay() {

        val hsReplayDescription = settingsView.findViewById<View>(R.id.hsReplayDescription) as TextView

        /*
         * state of the description
         */
        if (mHsReplayState.token == null) {
            hsReplayDescription.text = Utils.getString(R.string.hsReplayExplanation)
        } else {
            if (mHsReplayState.userName == null) {
                hsReplayDescription.text = Utils.getString(R.string.hsReplayExplanationNoUserName)
            } else {
                hsReplayDescription.text = Utils.getString(R.string.hsReplayExplanationWithUserName, mHsReplayState.userName as String)
            }
        }

        /*
         * state of the 2nd button
         * we do this one first because this is the enable/disable one (most important goes last)
         */
        if (mHsReplayState.token == null) {
            if (mHsReplayState.tokenLoading) {
                mHsReplayCompanion2!!.setLoading()
            } else {
                mHsReplayCompanion2!!.setText(Utils.getString(R.string.hsReplayEnable)) { v ->
                    HSReplay.get()
                            .createToken()
                            .subscribe({ this.handleTokenLce(it) })
                }
            }
        } else {
            mHsReplayCompanion2!!.setText(Utils.getString(R.string.hsReplayDisable)) { v ->
                mHsReplayState.userName = null
                mHsReplayState.token = null

                FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("hsreplay_disable", null)

                HSReplay.get().unlink()
                updateHsReplay()
            }
        }

        /*
         * state of the 1st button
         */
        if (mHsReplayState.token == null) {
            mHsReplayCompanion1!!.view().visibility = GONE
        } else {
            mHsReplayCompanion1!!.view().visibility = VISIBLE
            if (mHsReplayState.userNameLoading || mHsReplayState.claimUrlLoading) {
                mHsReplayCompanion1!!.setLoading()
            } else if (mHsReplayState.userName != null) {
                mHsReplayCompanion1!!.setText(Utils.getString(R.string.openInBrowser)) { v ->
                    ViewManager.Companion.get().removeView(settingsView)

                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://hsreplay.net/games/mine/")
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ArcaneTrackerApplication.context.startActivity(i)
                }
            } else {
                mHsReplayCompanion1!!.setText(Utils.getString(R.string.hsReplayClaim)) { v ->
                    ViewManager.Companion.get().removeView(settingsView)

                    OauthInterceptor.startOauth()
                }
            }
        }
    }

    private fun handleClaimUrlLce(lce: net.mbonnin.arcanetracker.hsreplay.model.Lce<String>) {
        if (lce.isLoading) {
            mHsReplayState.claimUrlLoading = true
        } else if (lce.error != null) {
            mHsReplayState.claimUrlLoading = false
            Toast.makeText(ArcaneTrackerApplication.context, Utils.getString(R.string.hsReplayClaimFailed), Toast.LENGTH_LONG).show()
            Utils.reportNonFatal(Exception("HSReplay claim url", lce.error))
        } else if (lce.data != null) {
            mHsReplayState.claimUrlLoading = false

            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("hsreplay_claimurl_opened", null)

            ViewManager.Companion.get().removeView(settingsView)

            Utils.openLink(lce.data)
        }

        updateHsReplay()
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
            val viewManager = ViewManager.Companion.get()
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
