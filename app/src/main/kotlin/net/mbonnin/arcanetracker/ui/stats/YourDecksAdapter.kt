package net.mbonnin.arcanetracker.ui.stats

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.util.getPlayerClass
import net.mbonnin.arcanetracker.DeckMapper
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.getPlayerClassColor
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import timber.log.Timber


class YourDecksAdapter : RecyclerView.Adapter<YourDecksAdapter.ViewHolder>() {
    val list = mutableListOf<Deck>()
    var listPopupAdapter: ListAdapter? = null

    init {
        RDatabaseSingleton.instance.deckDao().getCollection()
                .subscribeOn(Schedulers.io())
                .map { rdeckList ->
                    rdeckList
                            .sortedBy { it.name.toLowerCase() }
                            .map { rDeck -> DeckMapper.fromRDeck(rDeck) }
                            .filterNotNull()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    list.clear()
                    list.addAll(it)
                    notifyDataSetChanged()
                }, Timber::e)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_deck, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deck = list[position]

        holder.deckBackground.setBackgroundColor(getPlayerClassColor(getPlayerClass(deck.classIndex)))
        holder.deckName.setText(deck.name)
        holder.stats.setText("${deck.wins} - ${deck.losses}")
        holder.overflow.setOnClickListener {
            displayPopup(it, deck.id!!)
        }
    }

    fun displayPopup(view: View, deckId: String) {
        val popupWindow = ListPopupWindow(view.context)
        if (listPopupAdapter == null) {
            listPopupAdapter = ArrayAdapter<String>(view.context, android.R.layout.select_dialog_item, listOf(view.context.getString(R.string.delete)))

        }
        popupWindow.anchorView = view
        popupWindow.width = 120.toPixel(view.resources.displayMetrics)
        popupWindow.setDropDownGravity(Gravity.END)
        popupWindow.setAdapter(listPopupAdapter)
        popupWindow.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                deleteDeck(deckId)
                popupWindow.dismiss()
            }
        })

        popupWindow.isModal = true
        popupWindow.show()
    }

    private var onDeckClicked: ((String) -> Unit)? = null

    private fun deleteDeck(deckId: String) {
        Completable.fromAction { RDatabaseSingleton.instance.deckDao().delete(deckId) }
                .subscribeOn(Schedulers.io())
                .subscribe({}, {
                    Utils.reportNonFatal(it)
                })
    }

    fun setOnClickListener(onDeckClicked: (String) -> Unit) {
        this.onDeckClicked = onDeckClicked

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deckBackground = view.findViewById<ImageView>(R.id.deckBackground)
        val deckName = view.findViewById<TextView>(R.id.deckName)
        val stats = view.findViewById<TextView>(R.id.stats)
        val overflow = view.findViewById<View>(R.id.overflow)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position >= 0) {
                    onDeckClicked?.invoke(list[position].id!!)
                }
            }
        }
    }
}

