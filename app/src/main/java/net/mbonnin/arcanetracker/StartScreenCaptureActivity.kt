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
import net.mbonnin.arcanetracker.databinding.StartScreenCaptureActivityBinding

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class StartScreenCaptureActivity : AppCompatActivity() {
    private var mProjectionManager: MediaProjectionManager? = null

    private lateinit var binding: StartScreenCaptureActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Settings.get(Settings.SCREEN_CAPTURE_RATIONALE_SHOWN, false)) {
            setContentView(View(this))
            startActivity()
        } else {
            binding = StartScreenCaptureActivityBinding.inflate(android.view.LayoutInflater.from(this))
            setContentView(binding.root)

            val clickListener = { _: View ->
                startActivity()
                Settings.set(Settings.SCREEN_CAPTURE_RATIONALE_SHOWN, true)
            }

            binding.next.setOnClickListener(clickListener)
        }
    }

    override fun onResume() {
        super.onResume()
        this.makeFullscreen()
    }

    private fun startActivity() {
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