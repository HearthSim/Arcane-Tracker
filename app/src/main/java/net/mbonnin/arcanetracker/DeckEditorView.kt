package net.mbonnin.arcanetracker

import android.content.Context
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import net.mbonnin.arcanetracker.adapter.EditableItemAdapter
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.PlayerClass
import java.util.*

class DeckEditorView : RelativeLayout {

    internal lateinit var cardsRecyclerView: RecyclerView
    internal lateinit var deckRecyclerView: RecyclerView
    internal lateinit var manaSelectionView: ManaSelectionView
    internal lateinit var editText: EditText
    internal lateinit var cardCount: TextView
    internal lateinit var button: Button
    internal lateinit var classImageView: View
    internal lateinit var neutralImageView: View
    private var mDeck: Deck? = null
    private var mCardsAdapter: CardsAdapter? = null
    private var mDeckAdapter: EditableItemAdapter? = null
    private val mCardsAdapterListener = object: CardsAdapter.Listener {
        override fun onClick(card: Card) {
            mDeckAdapter!!.addCard(card.id)
            updateCardCount()
        }
    }

    private var close: ImageButton? = null

    private val mDeckAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()

            val list = mDeckAdapter!!.disabledCards
            mCardsAdapter!!.setDisabledCards(list)
            updateCardCount()
        }
    }

    private var classImageViewDisabled: View? = null
    private var neutralImageViewDisabled: View? = null
    private var mClass: String? = null

    private fun updateCardCount() {
        cardCount.text = mDeck!!.cardCount.toString() + "/ 30"
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onFinishInflate() {
        super.onFinishInflate()

        cardsRecyclerView = findViewById(R.id.cardsRecyclerView)
        deckRecyclerView = findViewById(R.id.deckRecyclerView)
        manaSelectionView = findViewById(R.id.manaSelectionView)
        editText = findViewById(R.id.editText)
        cardCount = findViewById(R.id.cardCount)
        button = findViewById(R.id.button)
        close = findViewById(R.id.close)
        classImageView = findViewById(R.id.classImageView)
        neutralImageView = findViewById(R.id.neutralImageView)
        classImageViewDisabled = findViewById(R.id.classImageViewDisabled)
        neutralImageViewDisabled = findViewById(R.id.neutralImageViewDisabled)

        findViewById<View>(R.id.filters).setOnTouchListener { v, event -> true }
    }

    fun setDeck(deck: Deck) {
        mDeck = deck

        val names = arrayOfNulls<String>(2)
        val playerClass = getPlayerClass(mDeck!!.classIndex)
        names[0] = playerClass.substring(0, 1) + playerClass.substring(1).toLowerCase()
        names[1] = "Neutral"

        mCardsAdapter = CardsAdapter()
        cardsRecyclerView.layoutManager = GridLayoutManager(context, 4)
        cardsRecyclerView.adapter = mCardsAdapter

        mDeckAdapter = EditableItemAdapter()
        mDeckAdapter!!.setDeck(deck)
        mDeckAdapter!!.registerAdapterDataObserver(mDeckAdapterObserver)
        val list = mDeckAdapter!!.disabledCards
        mCardsAdapter!!.setDisabledCards(list)

        deckRecyclerView.layoutManager = LinearLayoutManager(context)
        deckRecyclerView.adapter = mDeckAdapter


        mClass = playerClass

        mCardsAdapter!!.setListener(mCardsAdapterListener)
        mCardsAdapter!!.setClass(mClass!!)

        classImageView.setBackgroundDrawable(Utils.getDrawableForNameDeprecated(String.format(Locale.ENGLISH, "hero_%02d_round", mDeck!!.classIndex + 1)))
        classImageViewDisabled!!.visibility = View.GONE
        neutralImageView.setBackgroundResource(R.drawable.hero_10_round)


        classImageView.setOnClickListener { v ->
            mCardsAdapter!!.setClass(mClass!!)
            cardsRecyclerView.scrollToPosition(0)
            classImageViewDisabled!!.visibility = View.GONE
            neutralImageViewDisabled!!.visibility = View.VISIBLE
        }

        neutralImageView.setOnClickListener { v ->
            mCardsAdapter!!.setClass(PlayerClass.NEUTRAL)
            cardsRecyclerView.scrollToPosition(0)
            classImageViewDisabled!!.visibility = View.VISIBLE
            neutralImageViewDisabled!!.visibility = View.GONE
        }

        manaSelectionView.setListener { index ->
            mCardsAdapter!!.setCost(index)
            cardsRecyclerView.scrollToPosition(0)
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                mCardsAdapter!!.setSearchQuery(editText.text.toString())
                cardsRecyclerView.scrollToPosition(0)
            }
        })

        editText.setOnEditorActionListener { v, id, ev ->
            if (id == EditorInfo.IME_ACTION_DONE) {
            }
            false
        }

        updateCardCount()

        close!!.setOnClickListener { v -> editText.setText("") }

        button.setOnClickListener { v ->
            LegacyDeckList.save()
            LegacyDeckList.saveArena()
            //LegacyDeckList.getPlayerGameDeck().clear();
            MainViewCompanion.legacyCompanion.deck = deck
            ViewManager.get().removeView(this)
        }
    }

    companion object {

        fun build(context: Context): DeckEditorView {
            return LayoutInflater.from(ContextThemeWrapper(context, R.style.AppTheme)).inflate(R.layout.deck_editor_view, null) as DeckEditorView
        }

        fun show(deck: Deck): DeckEditorView {
            val context = ArcaneTrackerApplication.context
            val deckEditorView = DeckEditorView.build(context)

            val viewManager = ViewManager.get()
            val params = ViewManager.Params()
            params.x = 0
            params.y = 0
            params.w = ViewManager.get().usableWidth
            params.h = ViewManager.get().usableHeight

            deckEditorView.setDeck(deck)
            viewManager.addModalAndFocusableView(deckEditorView, params)

            return deckEditorView
        }
    }
}
