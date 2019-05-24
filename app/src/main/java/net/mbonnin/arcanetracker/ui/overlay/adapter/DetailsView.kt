package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.databinding.EntityViewWithSecretsBinding
import net.mbonnin.arcanetracker.hslog.power.Entity
import net.mbonnin.arcanetracker.hslog.power.GameLogic
import net.mbonnin.arcanetracker.hslog.power.FormatType
import net.mbonnin.arcanetracker.hslog.power.GameType
import net.mbonnin.hsmodel.enum.Type

class DetailsView(context: Context) : ViewGroup(context) {

    private val imageHeight = (ViewManager.get().height / 1.5).toInt()
    private var imageWidth = imageHeight * CardRenderer.TOTAL_WIDTH / CardRenderer.TOTAL_HEIGHT
    private val imageView = ImageView(context)
    private val progressBar = ProgressBar(context)
    private var anchorY = 0
    private var marginTop = Utils.dpToPx(30)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val padding = Utils.dpToPx(5)

        val imageLeft = padding
        var imageTop = anchorY - imageHeight / 2
        if (imageTop < 0) {
            imageTop = 0
        }
        if (imageTop + imageHeight > b - t) {
            imageTop = b - t - imageHeight
        }

        imageView.layout(imageLeft, imageTop, imageLeft + imageWidth, imageTop + imageHeight)

        val progressBarLeft = (imageLeft + imageLeft + imageWidth) / 2 - progressBar.measuredWidth / 2
        val progressBarTop = (imageTop + imageTop + imageHeight) / 2 - progressBar.measuredHeight / 2

        progressBar.layout(progressBarLeft,
                progressBarTop,
                progressBarLeft + progressBar.measuredWidth,
                progressBarTop + progressBar.measuredHeight)

        var x = imageLeft + imageWidth + padding
        children.forEach {
            if (it == imageView || it == progressBar) {
                return@forEach
            }
            x += padding

            var entityTop = imageTop + marginTop
            if (entityTop + it.measuredHeight > b - t) {
                entityTop = b - t - it.measuredHeight
            }
            it.layout(x, entityTop, x + it.measuredWidth, entityTop + it.measuredHeight)

            x += it.measuredWidth
            x += padding
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        children.forEach {
            if (it == imageView) {
                it.measure(
                        MeasureSpec.makeMeasureSpec(imageWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(imageHeight, MeasureSpec.EXACTLY)
                )
            } else if (it == progressBar) {
                it.measure(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
            } else {
                val entityWidthMeasureSpec = if (it is TextView)
                    MeasureSpec.makeMeasureSpec(imageWidth, MeasureSpec.EXACTLY)
                else
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                it.measure(
                        entityWidthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(h - marginTop, MeasureSpec.AT_MOST)
                )
            }
        }

        setMeasuredDimension(w, h)
    }

    fun configure(cardId: String, entityList: List<Entity>, anchorY: Int) {

        removeAllViews()
        this.anchorY = anchorY


        if (cardId != "?") {
            addView(imageView)
            addView(progressBar)

            Picasso.with(context).load(Utils.getCardUrl(cardId)).into(object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
                    imageView.setImageBitmap(bitmap)
                    progressBar.visibility = View.GONE
                }

                override fun onBitmapFailed(errorDrawable: Drawable?) {

                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                }
            })
        }

        for (entity in entityList) {
            val builder = StringBuilder()

            if (Utils.isAppDebuggable) {
                builder.append(context.getString(R.string.card, entity.EntityID))
                builder.append("\n")
            }


            val cardType = entity.tags[Entity.KEY_CARDTYPE]
            if (entity.extra.drawTurn != -1) {
                if (!builder.isEmpty()) {
                    builder.append("\n")
                }
                builder.append(context.getString(R.string.drawnTurn, GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn)))
                if (entity.extra.mulliganed) {
                    builder.append(" (")
                    builder.append(context.getString(R.string.mulliganed))
                    builder.append(")")
                }
            }

            if (entity.extra.playTurn != -1) {
                if (!builder.isEmpty()) {
                    builder.append("\n")
                }
                builder.append(context.getString(R.string.playedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.playTurn)))
            }
            if (entity.extra.diedTurn != -1 && (Type.MINION == cardType || Type.WEAPON == cardType)) {
                if (!builder.isEmpty()) {
                    builder.append("\n")
                }
                builder.append(context.getString(R.string.diedTurn, GameLogic.gameTurnToHumanTurn(entity.extra.diedTurn)))
            }
            if (!TextUtils.isEmpty(entity.extra.createdBy)) {
                if (!builder.isEmpty()) {
                    builder.append("\n")
                }
                builder.append(context.getString(R.string.createdBy, CardUtil.getCard(entity.extra.createdBy!!).name))
            }

            val textView: TextView
            val entityView: View
            if (Entity.ZONE_SECRET == entity.tags[Entity.KEY_ZONE] && entity.CardID.isNullOrEmpty()) {
                val binding = EntityViewWithSecretsBinding.inflate(LayoutInflater.from(context))
                entityView = binding.root
                textView = binding.textView

                if (!builder.isEmpty()) {
                    builder.append("\n")
                }
                builder.append(Utils.getString(R.string.possibleSecrets))
                builder.append("\n")

                appendPossibleSecrets(binding.secrets, entity)
            } else {
                textView = LayoutInflater.from(context).inflate(R.layout.entity_view, this, false) as TextView
                entityView = textView
            }


            if (builder.isEmpty()) {
                builder.append(Utils.getString(R.string.inDeck))
                builder.append("\n")
            }
            val s = builder.toString()

            textView.text = s

            addView(entityView)
        }
    }

    @Suppress("ConstantConditionIf")
    private fun appendPossibleSecrets(flexboxLayout: FlexboxLayout, entity: Entity) {
        val game = ArcaneTrackerApplication.get().hsLog.currentOrFinishedGame()

        val possibleSecrets: Collection<String>

        possibleSecrets = if (TestSwitch.SECRET_LAYOUT) {
            CardUtil.possibleSecretList(entity.tags[Entity.KEY_CLASS], GameType.GT_RANKED.name, FormatType.FT_WILD.name)
        } else {
            if (game == null) {
                return
            }
            CardUtil.possibleSecretList(entity.tags[Entity.KEY_CLASS], game.gameType, game.formatType)
        }

        val list = possibleSecrets.map {
            DeckEntryItem(card = CardUtil.getCard(it),
                    count = if (entity.extra.excludedSecretList.contains(it)) 0 else 1,
                    entityList = emptyList(),
                    gift = false)
        }

        for (deckEntryItem in list) {
            val view = LayoutInflater.from(context).inflate(R.layout.bar_card, null)
            val barTemplate = LayoutInflater.from(context).inflate(R.layout.bar_template, null) as ViewGroup
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            barTemplate.addView(view, 0, params)

            val holder = DeckEntryHolder(barTemplate)
            holder.bind(deckEntryItem)

            flexboxLayout.addView(barTemplate, ViewGroup.LayoutParams(imageWidth - Utils.dpToPx(40), Utils.dpToPx(30)))
        }
    }
}
