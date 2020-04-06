package net.mbonnin.arcanetracker.ui.my_packs

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_packs_header.*
import net.mbonnin.arcanetracker.R

class PacksHeaderViewHolder(override val containerView: View) : LayoutContainer, RecyclerView.ViewHolder(containerView) {
    fun bind(item: Item) {
        val resId = when(item) {
            is PacksItem -> R.drawable.pack
            is DustItem -> R.drawable.dust
            is DustAverageItem -> R.drawable.rightarrow
            else -> R.drawable.pack
        }

        imageView.setImageResource(resId)

        val num = when (item) {
            is PacksItem -> item.packs
            is DustItem -> item.dust
            is DustAverageItem -> item.average
            else -> 0
        }
        number.setText(num.toString())

        val desc = when(item) {
            is PacksItem -> R.string.packs_opened
            is DustItem -> R.string.dust
            is DustAverageItem -> R.string.average
            else -> 0
        }

        when (item) {
            is PacksItem,
            is DustItem -> imageView.rotation = 20f
            else -> imageView.rotation = 0f
        }

        description.setText(desc)
    }
}