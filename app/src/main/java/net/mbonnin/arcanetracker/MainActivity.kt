package net.mbonnin.arcanetracker

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.iid.FirebaseInstanceId
import net.mbonnin.arcanetracker.extension.finishAndRemoveTaskIfPossible
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var activityView: View
    private var checkbox: CheckBox? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Utils.logWithDate("MainActivity.onCreate")

        try {
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                Timber.d("Firebase token: " + FirebaseInstanceId.getInstance().token)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        activityView = findViewById(R.id.activity_main)
        val button = findViewById<View>(R.id.button) as Button
        checkbox = findViewById<View>(R.id.checkbox) as CheckBox
        val permissions = findViewById<View>(R.id.permissions)

        val showNextTime = Settings.get(Settings.SHOW_NEXT_TIME, true)
        checkbox!!.isChecked = Settings.get(Settings.SHOW_NEXT_TIME, showNextTime)

        if (hasAllPermissions()) {
            permissions.visibility = View.GONE
            button.text = getString(R.string.play)
        } else {
            button.text = getString(R.string.authorizeAndPlay)
        }

        button.setOnClickListener { _ -> tryToLaunchGame() }

        if (!showNextTime) {
            tryToLaunchGame()
            return
        }

        InAppBilling.get()
    }

    private fun hasAllPermissions(): Boolean {
        var has = checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            has = has and canReallyDrawOverlays()
        }

        return has
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!canReallyDrawOverlays()) {
            Snackbar.make(activityView, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show()
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
            Snackbar.make(activityView, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show()
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
            if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSIONS)
                return
            } else if (!canReallyDrawOverlays()) {
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

        launchGame()
    }

    private fun launchGame() {
        val hsIntent = packageManager.getLaunchIntentForPackage(HEARTHSTONE_PACKAGE_ID)
        if (hsIntent != null) {
            Settings.set(Settings.SHOW_NEXT_TIME, checkbox!!.isChecked)

            try {
                resources.openRawResource(R.raw.log_config).bufferedReader().use {
                    val logConfig = it.readText()
                    File(Utils.hsExternalDir + "log.config").writeText(logConfig)
                }
            } catch (e: Exception) {
                Snackbar.make(activityView, getString(R.string.cannot_locate_heathstone_install), Snackbar.LENGTH_LONG).show()
                Utils.reportNonFatal(Exception("cannot locate hearthstone install directory", e))
            }

            try {
                packageManager.getPackageInfo(HEARTHSTONE_PACKAGE_ID, 0)?.let {
                    val c = it.versionName.split(".")
                    try {
                        HDTApplication.get().hearthstoneBuild = c[c.size - 1].toInt()
                    } catch (e: Exception) {
                        Timber.e("cannot parse hearthstone version ${it.versionName}")
                    }

                    Timber.d("hearthstone build: ${HDTApplication.get().hearthstoneBuild}")
                }
            } catch (e: Exception) {
                Timber.d("Cannot find Hearthstone build number")
            }

            startActivity(hsIntent)
            finishAndRemoveTaskIfPossible()

            Overlay.get().show()

            Settings.set(Settings.CHECK_IF_RUNNING, false)
        } else {
            Snackbar.make(activityView, getString(R.string.cannot_launch), Snackbar.LENGTH_LONG).show()
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
