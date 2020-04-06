package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import net.hearthsim.hslog.DeckEntry
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils

/**
 * Created by martin on 10/17/16.
 */

class ItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal var list: List<DeckEntry> = emptyList()

    fun setList(list: List<DeckEntry>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val o = list[position]
        return when (o) {
            is DeckEntry.Hero -> TYPE_HERO
            is DeckEntry.Item -> TYPE_DECK_ENTRY
            is DeckEntry.Unknown -> TYPE_STRING
            else -> TYPE_HEADER
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val view: View
        when (viewType) {
            TYPE_HERO -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.bar_hero, null)
            }
            TYPE_DECK_ENTRY -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.bar_card, null)
            }
            TYPE_STRING -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.bar_text, null)
            }
            TYPE_HEADER -> {
                view = LayoutInflater.from(context).inflate(R.layout.bar_header, null)
            }
            else -> throw Exception("impossibleu!")
        }

        val barTemplate = LayoutInflater.from(context).inflate(R.layout.bar_template, null) as ViewGroup
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        barTemplate.addView(view, 0, params)

        when (viewType) {
            TYPE_DECK_ENTRY -> return DeckEntryHolder(barTemplate)
            TYPE_HERO -> return HeroHolder(barTemplate)
            else -> return object : RecyclerView.ViewHolder(barTemplate) {

            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val o = list[position]
        when (o) {
            is DeckEntry.Hero -> {
                (holder as HeroHolder).bind(o)
            }
            is DeckEntry.Item -> {
                (holder as DeckEntryHolder).bind(o)
            }
            is DeckEntry.Unknown -> {
                val barTemplate = holder.itemView as ViewGroup
                (barTemplate.getChildAt(0) as TextView).text = ArcaneTrackerApplication.context.getString(R.string.unknown_cards, o.count)
            }
            else -> {
                val textView = holder.itemView.findViewById<TextView>(R.id.textView)
                textView.typeface = ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.chunkfive)
                textView.text = when (o) {
                    is DeckEntry.PlayerDeck -> Utils.getString(R.string.deck)
                    is DeckEntry.Secrets -> Utils.getString(R.string.secrets)
                    is DeckEntry.OpponentDeck -> Utils.getString(R.string.allCards)
                    is DeckEntry.Hand -> "${Utils.getString(R.string.hand)} (${o.count})"
                    is DeckEntry.Text -> o.text
                    else -> ""
                }
            }
        }

        if (o is DeckEntry.Text) {
            holder.itemView.setOnClickListener {
                o.onClick?.invoke()
            }
        }
        val params2 = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30))
        holder.itemView.layoutParams = params2
    }

    override fun getItemCount(): Int {
        return list.size
    }

    companion object {
        internal val TYPE_DECK_ENTRY = 0
        internal val TYPE_STRING = 1
        internal val TYPE_HEADER = 2
        internal val TYPE_HERO = 3
    }
}
