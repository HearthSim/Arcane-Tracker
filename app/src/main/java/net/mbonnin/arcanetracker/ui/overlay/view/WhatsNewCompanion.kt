package net.mbonnin.arcanetracker.ui.overlay.view

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import net.mbonnin.arcanetracker.*

class WhatsNewCompanion(view: View, previousVersion: Int) {
    init {

        val context = HDTApplication.context

        val params = ViewManager.Params()
        params.x = ViewManager.get().width / 4
        params.y = ViewManager.get().height / 16
        params.w = ViewManager.get().width / 2
        params.h = 7 * ViewManager.get().height / 8

        val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
        checkBox.isChecked = true

        var v = BuildConfig.VERSION_CODE

        val linearLayout = view.findViewById<LinearLayout>(R.id.changelog)
        var foundChangelog = 0
        while (v > previousVersion) {
            val id = context.resources.getIdentifier("changelog_$v", "string", context.packageName)
            if (id > 0) {
                val c = context.getString(id)
                var textView = TextView(context)
                textView.textSize = 24f
                textView.setTextColor(Color.WHITE)
                textView.text = context.getString(R.string.cVersion, Integer.toString(v))
                textView.typeface = Typeface.DEFAULT_BOLD

                linearLayout.addView(textView)
                foundChangelog++

                val items = c.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (item in items) {
                    val s = item.split("\\{-\\}".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    textView = TextView(context)
                    textView.textSize = 16f
                    textView.setTextColor(Color.WHITE)
                    textView.text = "• ${s[0]}"
                    linearLayout.addView(textView)

                    if (s.size > 1) {
                        textView = TextView(context)
                        textView.textSize = 12f
                        textView.setTextColor(Color.WHITE)
                        textView.text = s[1]
                        linearLayout.addView(textView)
                    }
                }
            }

            v--
        }
        if (foundChangelog > 0) {
            view.findViewById<View>(R.id.ok).setOnClickListener { unused ->
                ViewManager.get().removeView(view)
                Settings[Settings.SHOW_CHANGELOG] = checkBox.isChecked
            }
            ViewManager.get().addModalView(view, params)
        }
    }
}