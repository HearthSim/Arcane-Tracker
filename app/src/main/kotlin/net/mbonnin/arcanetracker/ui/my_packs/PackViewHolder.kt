package net.mbonnin.arcanetracker.ui.my_packs

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_pack.*
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.helper.SetHelper
import net.mbonnin.arcanetracker.room.RPack
import net.mbonnin.arcanetracker.ui.stats.toPixel
import net.hearthsim.hsmodel.CardJson
import java.text.SimpleDateFormat
import java.util.*

class PackViewHolder(override val containerView: View) : LayoutContainer, RecyclerView.ViewHolder(containerView) {
    fun bind(rpack: RPack) {
        containerView.layoutParams.height = 32.toPixel(containerView.resources.displayMetrics)

        val cardList = rpack.cardList.split(",")

        date.setText(SimpleDateFormat.getDateInstance().format(Date(rpack.timeMillis)))

        var hsSet:String? = null
        for (i in 0 until 5) {
            val packCardView = (containerView as ViewGroup).getChildAt(3 + i) as PackCardView

            if (i < cardList.size) {
                var cardId = cardList[i]
                var golden = false
                if (cardId.endsWith("*")) {
                    cardId = cardId.substring(0, cardId.length - 1)
                    golden = true
                }
                val card = CardUtil.getCard(cardId)
                packCardView.setCard(card, golden)

                if (hsSet == null) {
                    hsSet = card.set
                }


            } else {
                packCardView.setCard(CardJson.UNKNOWN, false)
            }
        }

        if (hsSet != null) {
            val setDrawable = SetHelper.getDrawable(hsSet)
            setDrawable?.let{set.setImageDrawable(it)}
        }

        dust.setText(rpack.dust.toString())
    }
}