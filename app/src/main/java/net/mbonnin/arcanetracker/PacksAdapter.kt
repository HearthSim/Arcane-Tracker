package net.mbonnin.arcanetracker

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_pack.*
import net.mbonnin.arcanetracker.room.RPack
import java.text.SimpleDateFormat
import java.util.*

class PacksAdapter: PagedListAdapter<RPack, PacksAdapter.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pack, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rpack = getItem(position)

        if (rpack != null) {
            holder.bind(rpack)
        }
    }


    class ViewHolder(override val containerView: View): LayoutContainer, RecyclerView.ViewHolder(containerView) {
        fun bind(rpack: RPack) {
            val cardList = rpack.cardList.split(",")

            date.setText(SimpleDateFormat.getDateInstance().format(Date(rpack.timeMillis)))

            for (i in 0 until 5) {
                val textView = (containerView as ViewGroup).getChildAt(2 + i) as TextView

                if (i < cardList.size) {
                    var cardId = cardList[i]
                    var golden = false
                    if (cardId.endsWith("*")) {
                        cardId = cardId.substring(0, cardId.length - 1)
                        golden = true
                    }
                    val card = CardUtil.getCard(cardId)
                    textView.setText(card.name + (if (golden) "(golden)" else ""))

                    set.setText(card.set)
                } else {
                    textView.setText("?")
                }
            }
        }

    }
}

private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<RPack>() {
    override fun areContentsTheSame(oldItem: RPack?, newItem: RPack?): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: RPack?, newItem: RPack?): Boolean {
        return oldItem == newItem
    }
}