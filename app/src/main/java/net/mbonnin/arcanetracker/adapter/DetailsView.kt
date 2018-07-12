package net.mbonnin.arcanetracker.adapter

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.size
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.databinding.DetailsViewBinding
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.hsmodel.enum.Type

class DetailsView(context: Context) : LinearLayout(context) {
    private var mTopMargin: Int = 0
    private var mCardWidth: Int = 0

    init {
        orientation = LinearLayout.HORIZONTAL

    }

    fun configure(bitmap: Bitmap?, entityList: List<Entity>, height: Int) {

        val w = height * CardRenderer.TOTAL_WIDTH / CardRenderer.TOTAL_HEIGHT
        if (bitmap != null) {
            val imageView = ImageView(context)
            imageView.setImageBitmap(bitmap)
            val layoutParams = LinearLayout.LayoutParams(w, height)
            addView(imageView, layoutParams)
        }

        mCardWidth = w
        mTopMargin = 30

        for (entity in entityList) {
            val b = DetailsViewBinding.inflate(LayoutInflater.from(context))
            b.root.minimumWidth = mCardWidth

            val builder = StringBuilder()

            if (Utils.isAppDebuggable) {
                builder.append(context.getString(R.string.card, entity.EntityID))
                builder.append("\n")
            }


            val cardType = entity.tags[Entity.KEY_CARDTYPE]
            if (entity.extra.drawTurn != -1) {
                builder.append(context.getString(R.string.drawnTurn, GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn)))
                if (entity.extra.mulliganed) {
                    builder.append(" (")
                    builder.append(context.getString(R.string.mulliganed))
                    builder.append(")")
                }
                builder.append("\n")
            }

            if (entity.extra.playTurn != -1) {
                builder.append(context.getString(R.string.playedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.playTurn)))
                builder.append("\n")
            }
            if (entity.extra.diedTurn != -1 && (Type.MINION == cardType || Type.WEAPON == cardType)) {
                builder.append(context.getString(R.string.diedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.diedTurn)))
                builder.append("\n")
            }
            if (!TextUtils.isEmpty(entity.extra.createdBy)) {
                builder.append(context.getString(R.string.createdBy, CardUtil.getCard(entity.extra.createdBy!!).name))
            }

            if (Entity.ZONE_SECRET == entity.tags[Entity.KEY_ZONE] && entity.CardID.isNullOrEmpty()) {
                builder.append(Utils.getString(R.string.possibleSecrets))
                appendPossibleSecrets(b.secrets, entity)
            }

            var s = builder.toString()

            if (Utils.isEmpty(s)) {
                builder.append(Utils.getString(R.string.inDeck))
                builder.append("\n")
                s = builder.toString()
            }

            b.textView.text = s
            b.textView.typeface = Typefaces.franklin()

            addView(b.root)
        }

        applyMargins()
    }

    internal fun applyMargins() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ImageView) {
                continue
            }
            val layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            val p = Utils.dpToPx(5)
            layoutParams.rightMargin = p
            layoutParams.leftMargin = layoutParams.rightMargin
            layoutParams.topMargin = Utils.dpToPx(mTopMargin)
            child.layoutParams = layoutParams
        }
        requestLayout()
    }

    fun setTopMargin(topMargin: Int) {
        mTopMargin = topMargin
        applyMargins()
    }

    private fun appendPossibleSecrets(horizontalLayout: LinearLayout, entity: Entity) {
        val game = GameLogicListener.get().currentGame

        val possibleSecrets: Collection<String>

        if (TestSwitch.SECRET_LAYOUT) {
            possibleSecrets = CardUtil.possibleSecretList(entity.tags[Entity.KEY_CLASS], GameType.GT_RANKED.name, FormatType.FT_WILD.name)
        } else {
            if (game == null) {
                return
            }
            possibleSecrets = CardUtil.possibleSecretList(entity.tags[Entity.KEY_CLASS], game.gameType, game.formatType)
        }

        val list = possibleSecrets.map {
            val deckEntryItem = DeckEntryItem(card = CardUtil.getCard(it))
            deckEntryItem.count = if (entity.extra.excludedSecretList.contains(it)) 0 else 1

            deckEntryItem
        }

        var verticalLayout: LinearLayout? = null
        for (deckEntryItem in list) {

            if (verticalLayout == null) {
                verticalLayout = LinearLayout(horizontalLayout.context)
                verticalLayout.orientation = VERTICAL
                horizontalLayout.addView(verticalLayout, mCardWidth - Utils.dpToPx(40), WRAP_CONTENT)
            }
            val view = LayoutInflater.from(context).inflate(R.layout.bar_card, null)
            val barTemplate = LayoutInflater.from(context).inflate(R.layout.bar_template, null) as ViewGroup
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            barTemplate.addView(view, 0, params)

            val holder = DeckEntryHolder(barTemplate)
            holder.bind(deckEntryItem)

            verticalLayout.addView(barTemplate, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30)))

            if (verticalLayout.size >= 8) {
                // if we have a lot of secrets, they won't fit on a single screen
                verticalLayout = null
            }
        }
    }
}
