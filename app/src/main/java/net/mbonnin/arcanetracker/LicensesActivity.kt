package net.mbonnin.arcanetracker

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.InputStreamReader

class LicensesActivity : Activity() {
    private lateinit var licenses: List<Pair<String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inputStream = this.resources.openRawResource(R.raw.licenses)
        val text = InputStreamReader(inputStream).readText()
        licenses = text.split("====\n")
                .map { it.substringBefore("\n") to it.substringAfter("\n") }

        val scrollView = ScrollView(this)
        val linearLayout = LinearLayout(this)
        scrollView.addView(linearLayout)
        linearLayout.orientation = LinearLayout.VERTICAL

        for (license in licenses) {
            val view = LayoutInflater.from(this).inflate(R.layout.license_item, linearLayout, false)
            val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            linearLayout.addView(view, params)
            view.findViewById<TextView>(R.id.tv1).text = license.first
            view.findViewById<TextView>(R.id.tv2).text = license.second


        }
        setContentView(scrollView)
    }
}
