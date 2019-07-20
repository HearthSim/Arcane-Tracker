package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils

import java.util.ArrayList

import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

/**
 * Created by martin on 10/17/16.
 */

class ItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal var list: List<Any> = ArrayList()

    fun setList(list: List<Any>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val o = list[position]
        if (o is DeckEntryItem) {
            return TYPE_DECK_ENTRY
        } else if (o is String) {
            return TYPE_STRING
        } else if (o is HeaderItem) {
            return TYPE_HEADER
        }

        Timber.e("unsupported type at position %d: %s", position, o.toString())
        return -1
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val view: View
        when (viewType) {
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
            else -> return object : RecyclerView.ViewHolder(barTemplate) {

            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val o = list[position]
        if (o is DeckEntryItem) {
            (holder as DeckEntryHolder).bind(o)
        } else if (o is String) {
            val barTemplate = holder.itemView as ViewGroup
            (barTemplate.getChildAt(0) as TextView).text = o
        } else if (o is HeaderItem) {
            val textView = holder.itemView.findViewById<TextView>(R.id.textView)
            textView.typeface = ResourcesCompat.getFont(ArcaneTrackerApplication.context, R.font.chunkfive)
            val text = ""//headerItem.expanded ?"▼":"▶";
            textView.text = text + o.title
            if (o.onClicked != null) {
                textView.setOnClickListener { v -> o.onClicked?.run() }
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
    }


}
