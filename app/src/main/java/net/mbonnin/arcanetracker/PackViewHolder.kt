package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_pack.*
import net.mbonnin.arcanetracker.room.RPack
import java.text.SimpleDateFormat
import java.util.*

class PackViewHolder(override val containerView: View) : LayoutContainer, RecyclerView.ViewHolder(containerView) {
    fun bind(rpack: RPack) {
        containerView.layoutParams.height = 32.toPixel(containerView.resources.displayMetrics)

        val cardList = rpack.cardList.split(",")

        date.setText(SimpleDateFormat.getDateInstance().format(Date(rpack.timeMillis)))

        for (i in 0 until 5) {
            val packCardView = (containerView as ViewGroup).getChildAt(2 + i) as PackCardView

            if (i < cardList.size) {
                var cardId = cardList[i]
                var golden = false
                if (cardId.endsWith("*")) {
                    cardId = cardId.substring(0, cardId.length - 1)
                    golden = true
                }
                val card = CardUtil.getCard(cardId)
                packCardView.setCard(card, golden)

                set.setText(card.set)
            } else {
                packCardView.setCard(CardUtil.UNKNOWN, false)
            }
        }
    }
}