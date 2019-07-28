package net.mbonnin.arcanetracker.ui.overlay.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexboxLayout
import net.hearthsim.hslog.PossibleSecret
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ui.overlay.adapter.DeckEntryHolder
import net.hearthsim.hslog.DeckEntry
import net.mbonnin.arcanetracker.CardUtil

class TopDrawerCompanion {
    val context = ArcaneTrackerApplication.get()

    val view = LayoutInflater.from(context).inflate(R.layout.top_drawer, null, false)

    val secretFlexbox = view.findViewById<FlexboxLayout>(R.id.secretFlexbox)
    val infoText = view.findViewById<TextView>(R.id.infoText)


    val handles = HandlesView(context)
    val drawerHelper = DrawerHelper(view, handles, DrawerHelper.Edge.TOP)

    val secretHandle: HandleView
    val infoHandle: HandleView

    init {
        drawerHelper.setViewHeight(Utils.dpToPx(4 * BAR_HEIGHT))
        drawerHelper.setViewWidth(Utils.dpToPx(3 * BAR_WIDTH))

        infoText.visibility = View.GONE

        secretHandle = HandleView(context)
        val drawable = context.resources.getDrawable(R.drawable.ic_question)
        secretHandle.init(drawable, Color.BLACK)
        secretHandle.setOnClickListener {

            if (!infoText.isVisible && drawerHelper.isOpen()) {
                drawerHelper.close()
            } else {
                infoText.visibility = View.GONE
                drawerHelper.open()
            }
        }
        secretHandle.visibility = View.GONE

        infoHandle = HandleView(context)
        infoHandle.init(context.resources.getDrawable(R.drawable.ic_info), context.resources.getColor(R.color.hsReplayBlue))
        infoHandle.setOnClickListener {

            if (infoText.isVisible && drawerHelper.isOpen()) {
                drawerHelper.close()
            } else {
                infoText.visibility = View.VISIBLE
                drawerHelper.open()
            }
        }
        infoHandle.visibility = View.GONE

        handles.orientation = LinearLayout.HORIZONTAL
        handles.addView(secretHandle)

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = Utils.dpToPx(10)
        handles.addView(infoHandle, params)

    }

    companion object {
        const val BAR_HEIGHT = 30
        const val BAR_WIDTH = 120
    }

    var secrets: List<PossibleSecret> = emptyList()

    fun setPossibleSecrets(possibleSecrets: List<PossibleSecret>) {
        secrets = possibleSecrets

        secretFlexbox.removeAllViews()

        possibleSecrets.forEach { secret ->
            val view = LayoutInflater.from(context).inflate(R.layout.bar_card, null)
            val barTemplate = LayoutInflater.from(context).inflate(R.layout.bar_template, null) as ViewGroup
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            barTemplate.addView(view, 0, params)

            val deckEntry = DeckEntry.Item(
                    card = CardUtil.getCard(secret.cardId),
                    count = Math.max(secret.count, 1),
                    entityList = emptyList()
            )
            val holder = DeckEntryHolder(barTemplate)
            holder.bind(deckEntry)

            secretFlexbox.addView(barTemplate, ViewGroup.LayoutParams(Utils.dpToPx(BAR_WIDTH), Utils.dpToPx(BAR_HEIGHT)))
        }
        update()
    }

    var text = ""

    fun setInfo(text: String) {
        this.text = text
    }

    fun update() {
        drawerHelper.show(!secrets.isEmpty() || text.isNotEmpty())

        secretHandle.visibility = if (secrets.isEmpty()) View.GONE else View.VISIBLE
        infoHandle.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE

        drawerHelper.notifyHandlesChanged()
    }
}