package net.mbonnin.arcanetracker.adapter

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.databinding.DetailsViewBinding
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.hsmodel.CardId
import net.mbonnin.hsmodel.PlayerClass
import net.mbonnin.hsmodel.Type
import java.util.*

class DetailsView(context: Context) : LinearLayout(context) {
    private var mTopMargin: Int = 0
    private var mCardWidth: Int = 0

    init {
        orientation = LinearLayout.HORIZONTAL

    }

    fun configure(bitmap: Bitmap?, deckEntryItem: DeckEntryItem, height: Int) {

        val w = height * CardRenderer.TOTAL_WIDTH / CardRenderer.TOTAL_HEIGHT
        if (bitmap != null) {
            val imageView = ImageView(context)
            imageView.setImageBitmap(bitmap)
            val layoutParams = LinearLayout.LayoutParams(w, height)
            addView(imageView, layoutParams)
        }

        mCardWidth = w
        mTopMargin = 30

        for (entity in deckEntryItem.entityList) {
            val b = DetailsViewBinding.inflate(LayoutInflater.from(context))

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
                builder.append(context.getString(R.string.createdBy, CardUtil.getCard(entity.extra.createdBy).name))
            }

            if (Entity.ZONE_SECRET == entity.tags[Entity.KEY_ZONE] && TextUtils.isEmpty(entity.CardID)) {
                builder.append(Utils.getString(R.string.possibleSecrets))
                appendPossibleSecrets(b.root as LinearLayout, entity)
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
            val layoutParams = LinearLayout.LayoutParams(mCardWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
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

    private fun appendPossibleSecrets(verticalLayout: LinearLayout, entity: Entity) {
        val playerClass = entity.tags[Entity.KEY_CLASS] ?: return

        val list = ArrayList<DeckEntryItem>()

        when (playerClass) {
            PlayerClass.HUNTER -> {
                addSecret(list, CardId.BEAR_TRAP, entity.extra.selfHeroAttacked)
                addSecret(list, CardId.CAT_TRICK, entity.extra.otherPlayerCastSpell)
                addSecret(list, CardId.EXPLOSIVE_TRAP, entity.extra.selfHeroAttacked)
                addSecret(list, CardId.FREEZING_TRAP, entity.extra.selfHeroAttacked || entity.extra.selfMinionWasAttacked)
                addSecret(list, CardId.SNAKE_TRAP, entity.extra.selfMinionWasAttacked)
                addSecret(list, CardId.SNIPE, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.DART_TRAP, entity.extra.otherPlayerHeroPowered)
                addSecret(list, CardId.HIDDEN_CACHE, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.MISDIRECTION, entity.extra.selfHeroAttacked)
                addSecret(list, CardId.VENOMSTRIKE_TRAP, entity.extra.selfMinionWasAttacked)
                addSecret(list, CardId.WANDERING_MONSTER, entity.extra.selfHeroAttacked)
            }
            PlayerClass.MAGE -> {
                addSecret(list, CardId.MIRROR_ENTITY, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.MANA_BIND, entity.extra.otherPlayerCastSpell)
                addSecret(list, CardId.COUNTERSPELL, entity.extra.otherPlayerCastSpell)
                addSecret(list, CardId.EFFIGY, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.ICE_BARRIER, entity.extra.selfHeroAttacked)
                addSecret(list, CardId.POTION_OF_POLYMORPH, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.DUPLICATE, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.VAPORIZE, entity.extra.selfHeroAttackedByMinion)
                addSecret(list, CardId.ICE_BLOCK, false)
                addSecret(list, CardId.SPELLBENDER, entity.extra.selfMinionTargetedBySpell)
                addSecret(list, CardId.FROZEN_CLONE, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.EXPLOSIVE_RUNES, entity.extra.otherPlayerPlayedMinion)
            }
            PlayerClass.PALADIN -> {
                addSecret(list, CardId.COMPETITIVE_SPIRIT, entity.extra.competitiveSpiritTriggerConditionHappened)
                addSecret(list, CardId.AVENGE, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.REDEMPTION, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.REPENTANCE, entity.extra.otherPlayerPlayedMinion)
                addSecret(list, CardId.SACRED_TRIAL, entity.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready)
                addSecret(list, CardId.NOBLE_SACRIFICE, entity.extra.selfHeroAttacked || entity.extra.selfMinionWasAttacked)
                addSecret(list, CardId.GETAWAY_KODO, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.EYE_FOR_AN_EYE, entity.extra.selfHeroAttacked)
            }
            PlayerClass.ROGUE -> {
                addSecret(list, CardId.CHEAT_DEATH, entity.extra.selfPlayerMinionDied)
                addSecret(list, CardId.SUDDEN_BETRAYAL, entity.extra.selfHeroAttacked)
                addSecret(list, CardId.EVASION, entity.extra.selfHeroDamaged)
            }
        }

        for (deckEntryItem in list) {
            val view = LayoutInflater.from(context).inflate(R.layout.bar_card, null)
            val barTemplate = LayoutInflater.from(context).inflate(R.layout.bar_template, null) as ViewGroup
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            barTemplate.addView(view, 0, params)

            val holder = DeckEntryHolder(barTemplate)
            holder.bind(deckEntryItem)

            verticalLayout.addView(barTemplate, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30)))
        }
    }

    private fun addSecret(list: MutableList<DeckEntryItem>, cardId: String, condition: Boolean) {
        val deckEntryItem = DeckEntryItem(card = CardUtil.getCard(cardId))
        deckEntryItem.count = if (condition) 0 else 1

        when (GameLogicListener.get().currentGame?.bnetGameType) {
            BnetGameType.BGT_RANKED_STANDARD,
            BnetGameType.BGT_CASUAL_STANDARD_NORMAL,
            BnetGameType.BGT_ARENA -> if (deckEntryItem.card.isStandard()) list.add(deckEntryItem)
            else -> list.add(deckEntryItem)
        }
    }
}
