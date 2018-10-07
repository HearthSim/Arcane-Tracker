package net.mbonnin.arcanetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View.GONE
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import net.mbonnin.arcanetracker.extension.makeFullscreen
import timber.log.Timber

class OauthResultActivity : AppCompatActivity() {

    lateinit var progressBar: ProgressBar
    lateinit var oauthResult: TextView
    lateinit var ok: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("Redirect => " + intent.data.toString())
        setContentView(R.layout.oauth_result_activity)

        progressBar = findViewById(R.id.progressBar)
        oauthResult = findViewById(R.id.oauth_result)
        ok = findViewById(R.id.ok)

        oauthResult.visibility = GONE
        ok.visibility = GONE


    }

    override fun onResume() {
        super.onResume()
        this.makeFullscreen()
    }
}