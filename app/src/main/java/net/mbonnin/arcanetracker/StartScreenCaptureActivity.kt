package net.mbonnin.arcanetracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.View

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StartScreenCaptureActivity : AppCompatActivity() {
    private var mProjectionManager: MediaProjectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), MainActivity.REQUEST_CODE_MEDIAPROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MainActivity.REQUEST_CODE_MEDIAPROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                val projection = mProjectionManager!!.getMediaProjection(resultCode, data)
                ScreenCaptureHolder.mediaProjectionCreated(projection)
                finish()
            } else {
                ScreenCaptureHolder.mediaProjectionAborted()
                AlertDialog.Builder(this)
                        .setTitle(getString(R.string.hi_there))
                        .setMessage(getString(R.string.noScreenCapture))
                        .setPositiveButton(getString(R.string.ok)) { dialog, which -> dialog.dismiss() }
                        .show()
                finish()
            }
        }
    }
}