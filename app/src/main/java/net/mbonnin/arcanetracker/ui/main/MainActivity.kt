package net.mbonnin.arcanetracker.ui.main

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.hearthsim.hsreplay.HsReplayOauthApi
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Settings
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.extension.finishAndRemoveTaskIfPossible
import net.mbonnin.arcanetracker.ui.overlay.Overlay
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private var job: Job? = null
    lateinit var container: FrameLayout

    var state = State(true, false, false)

    data class State(val needLogin: Boolean,
                     val loginLoading: Boolean,
                     val showNextTime: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = FrameLayout(this)
        setContentView(container)

        Utils.logWithDate("MainActivity.onCreate")

        try {
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                Timber.d("Firebase token: ${FirebaseInstanceId.getInstance().token}")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        val needLogin = ArcaneTrackerApplication.get().hsReplay.account() == null

        state = state.copy(showNextTime = Settings.get(Settings.SHOW_NEXT_TIME, true),
                needLogin = needLogin)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent?) {
        val url = intent?.data?.toString()
        if (url != null && url.startsWith(HsReplayOauthApi.CALLBACK_URL)) {
            val code = Uri.parse(url).getQueryParameter("code")
            if (code != null) {
                updateState(state.copy(needLogin = true, loginLoading = true))
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.Main) {
                    val result = ArcaneTrackerApplication.get().hsReplay.login(code)

                    result.fold(
                            onFailure = {
                                Utils.reportNonFatal(it)
                                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                                updateState(state.copy(needLogin = true, loginLoading = false))
                            },
                            onSuccess = {
                                updateState(state.copy(loginLoading = false, needLogin = false))
                            }
                    )
                }

                return
            }
        }
        updateState(state)
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    fun updateState(newState: State) {
        container.removeAllViews()

        if (newState.needLogin) {
            val view = LayoutInflater.from(this).inflate(R.layout.login_view, container, false)
            val loginCompanion = LoginCompanion(view)
            loginCompanion.loading(newState.loginLoading)
            loginCompanion.hasAllPermissions(hasAllPermissions())
            container.addView(view)
        } else if (hasAllPermissions()) {
            doLaunchGame()
        } else {
            val view = LayoutInflater.from(this).inflate(R.layout.permission_view, container, false)
            val button = view.findViewById<View>(R.id.button) as Button

            button.setOnClickListener { _ -> tryToLaunchGame() }

            container.addView(view)

            if (!newState.showNextTime) {
                tryToLaunchGame()
                return
            }
        }
        state = newState
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun hasAllPermissions(): Boolean {
        var has = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            has = has and canReallyDrawOverlays()
        }

        return has
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!canReallyDrawOverlays()) {
            Snackbar.make(container, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show()
        } else {
            tryToLaunchGame()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun canReallyDrawOverlays(): Boolean {
        return android.provider.Settings.canDrawOverlays(this)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(container, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show()
        } else {
            tryToLaunchGame()
        }
    }

    private fun tryToLaunchGame() {
        /*
         * Do not use the application context, dialogs do not work with an application context
         */
        val context = ContextThemeWrapper(this, R.style.AppThemeLight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!canReallyDrawOverlays()) {
                try {
                    val intent2 = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
                    startActivityForResult(intent2, REQUEST_CODE_GET_OVERLAY_PERMISSIONS)
                } catch (e: Exception) {
                    AlertDialog.Builder(context)
                            .setTitle(getString(R.string.hi_there))
                            .setMessage(getString(R.string.overlay_explanation))
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                            .show()
                }

                return
            }
        }


        if (Build.MANUFACTURER.toLowerCase().contains("xiaomi") && Settings.get(Settings.SHOW_XIAOMI_WARNING, true)) {
            AlertDialog.Builder(context)
                    .setTitle(getString(R.string.hi_there))
                    .setMessage(getString(R.string.xiaomi_explanation))
                    .setNeutralButton(getString(R.string.learn_more)) { _, _ -> Utils.openLink("https://www.reddit.com/r/arcanetracker/comments/5nygi0/read_this_if_you_are_playing_on_a_xiaomi_device/") }
                    .setPositiveButton(getString(R.string.gotIt)) { dialog, _ ->
                        dialog.dismiss()
                        Settings.set(Settings.SHOW_XIAOMI_WARNING, false)
                        tryToLaunchGame()
                    }
                    .show()
            return
        }

        if (Settings.get(Settings.CHECK_IF_RUNNING, true)) {
            AlertDialog.Builder(context)
                    .setTitle(getString(R.string.please_stop_hearthstone))
                    .setMessage(getString(R.string.stop_hearthstone_explanation))
                    .setPositiveButton(getString(R.string.continue_)) { dialog, _ ->
                        dialog.dismiss()
                        Settings.set(Settings.CHECK_IF_RUNNING, false)
                        tryToLaunchGame()
                    }
                    .show()
            return
        }

        doLaunchGame()
    }

    private fun writeLogConfig() {
        val logConfig = resources.openRawResource(R.raw.log_config).bufferedReader().use { it.readText() }
        val outputStream = contentResolver.openOutputStream(Uri.parse("content://com.blizzard.wtcg.hearthstone.exportedcontentprovider/log.config"))
        if (outputStream == null) {
            Snackbar.make(container, getString(R.string.cannot_write_log_config), Snackbar.LENGTH_LONG).show()
            Utils.reportNonFatal(Exception("cannot write log.config"))
        } else {
            Timber.d("Wrote log.config from the ContentProvider")
            outputStream.use {
                val writer = it.writer()
                writer.write(logConfig)
                writer.flush()
                writer.close()
            }
        }
    }

    private fun doLaunchGame() {
        val hsIntent = packageManager.getLaunchIntentForPackage(HEARTHSTONE_PACKAGE_ID)
        if (hsIntent != null) {
            Settings.set(Settings.SHOW_NEXT_TIME, false)

            writeLogConfig()

            try {
                packageManager.getPackageInfo(HEARTHSTONE_PACKAGE_ID, 0)?.let {
                    val c = it.versionName.split(".")
                    try {
                        ArcaneTrackerApplication.get().hearthstoneBuild = c[c.size - 1].toInt()
                    } catch (e: Exception) {
                        Timber.e("cannot parse hearthstone version ${it.versionName}")
                    }

                    Timber.d("hearthstone build: ${ArcaneTrackerApplication.get().hearthstoneBuild}")
                }
            } catch (e: Exception) {
                Timber.d("Cannot find Hearthstone build number")
            }

            startActivity(hsIntent)
            finishAndRemoveTaskIfPossible()

            Overlay.show()

            Settings.set(Settings.CHECK_IF_RUNNING, false)
        } else {
            Snackbar.make(container, getString(R.string.cannot_launch), Snackbar.LENGTH_LONG).show()
            Utils.reportNonFatal(Exception("no intent to launch game"))
        }
    }

    companion object {
        private val REQUEST_CODE_PERMISSIONS = 1
        private val REQUEST_CODE_GET_OVERLAY_PERMISSIONS = 2
        val REQUEST_CODE_MEDIAPROJECTION = 42
        val HEARTHSTONE_PACKAGE_ID = "com.blizzard.wtcg.hearthstone"
    }
}
