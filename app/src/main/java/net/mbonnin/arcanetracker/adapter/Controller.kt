package net.mbonnin.arcanetracker.adapter


import android.os.Handler
import android.text.TextUtils
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.EntityList
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.hsmodel.Rarity
import net.mbonnin.hsmodel.Type
import java.util.*

class Controller : GameLogic.Listener {


    val playerAdapter: ItemAdapter
    private val mHandler: Handler
    val opponentAdapter: ItemAdapter
    protected var mPlayerCardMap: HashMap<String, Int>? = null
    private var mGame: Game? = null
    private var mPlayerId: String? = null
    private var mOpponentId: String? = null

    private val mUpdateRunnable = Runnable { this.update() }

    private val hand: ArrayList<*>
        get() {
            val list = ArrayList<Any>()
            val context = ArcaneTrackerApplication.context

            val entities = getEntityListInZone(mOpponentId, Entity.ZONE_HAND)

            Collections.sort(entities) { a, b -> compareNullSafe(a.tags[Entity.KEY_ZONE_POSITION], b.tags[Entity.KEY_ZONE_POSITION]) }

            list.add(HeaderItem(context.getString(R.string.hand) + " (" + entities.size + ")"))
            for (entity in entities) {
                val deckEntry = DeckEntryItem()
                if (TextUtils.isEmpty(entity.CardID) || entity.extra.hide) {
                    val builder = StringBuilder()
                    builder.append("#").append(GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn))
                    if (entity.extra.mulliganed) {
                        builder.append(" (M)")
                    }
                    deckEntry.card = CardUtil.unknown(builder.toString())
                } else {
                    deckEntry.card = entity.card
                }
                deckEntry.gift = !entity.extra.hide && entity.extra.tmpIsGift
                deckEntry.count = 1
                val clone = entity.clone()
                if (entity.extra.hide) {
                    clone.extra.createdBy = null
                }
                deckEntry.entityList.add(clone)
                list.add(deckEntry)
            }

            return list
        }

    fun getSecrets(): List<Any> {
        val list = ArrayList<Any>()

        val entities = getEntityListInZone(mOpponentId, Entity.ZONE_SECRET)
                .filter { e -> Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] }

        Collections.sort(entities) { a, b -> compareNullSafe(a.tags[Entity.KEY_ZONE_POSITION], b.tags[Entity.KEY_ZONE_POSITION]) }

        for (entity in entities) {
            val deckEntry = DeckEntryItem()
            if (TextUtils.isEmpty(entity.CardID)) {
                val clazz = entity.tags[Entity.KEY_CLASS]

                if (clazz != null) {
                    deckEntry.card = CardUtil.secret(clazz)
                } else {
                    deckEntry.card = CardUtil.secret("MAGE")
                }
            } else {
                deckEntry.card = entity.card
            }
            deckEntry.gift = entity.extra.tmpIsGift
            deckEntry.count = 1

            val clone = entity.clone()
            clone.card = deckEntry.card
            deckEntry.entityList.add(clone)
            list.add(deckEntry)
        }

        return list
    }

    init {
        playerAdapter = ItemAdapter()
        opponentAdapter = ItemAdapter()
        GameLogic.get().addListener(this)
        mHandler = Handler()
    }

    fun setPlayerDeck(cardMap: HashMap<String, Int>) {
        mPlayerCardMap = cardMap
        update()
    }

    private fun entityListToItemList(entityList: EntityList, increasesCount: (Entity) -> Boolean): ArrayList<Any> {
        /*
         * remove and count the really unknown cards
         */
        var unknownCards = 0
        val it = entityList.iterator()
        while (it.hasNext()) {
            val entity = it.next()
            if (!entity.extra.tmpIsGift && entity.extra.tmpCard === CardUtil.UNKNOWN) {
                it.remove()
                unknownCards++
            }
        }

        val deckEntryItemMap = mutableMapOf<Pair<String, Boolean>, DeckEntryItem>()

        entityList.forEach { entity ->
            val deckEntryItem = deckEntryItemMap.getOrPut(entity.extra.tmpCard.id to entity.extra.tmpIsGift, {
                val deckEntryItem = DeckEntryItem()
                deckEntryItem.card = entity.extra.tmpCard
                deckEntryItem.gift = entity.extra.tmpIsGift
                deckEntryItem
            })

            deckEntryItem.entityList.add(entity)
            if (increasesCount(entity)) {
                deckEntryItem.count++
            }
        }

        val deckEntryItemList = deckEntryItemMap.values.sortedWith(Comparator { a, b ->
            val ret = Utils.compareNullSafe(a.card.cost, b.card.cost)

            if (ret != 0) {
                return@Comparator ret
            }

             a.card.name.compareTo(b.card.name)
        })

        /*
         * sort the entity list
         */
        for (deckEntryItem in deckEntryItemList) {
            deckEntryItem.entityList.sortedWith(Comparator { a, b -> a.extra.drawTurn - b.extra.drawTurn })
        }

        val itemList = ArrayList<Any>()
        itemList.addAll(deckEntryItemList)
        if (unknownCards > 0) {
            itemList.add(ArcaneTrackerApplication.context.getString(R.string.unknown_cards, unknownCards))
        }

        return itemList
    }

    /*
     * this attempts to map the knowledge that we have of mDeck to the unknown entities
     * this assumes that an original card is either known or still in deck
     * this sets tmpCard
     *
     * what needs to be handled is hemet, maybe others ?
     */
    private fun assignCardsFromDeck() {
        val originalDeckEntityList = mGame!!.getEntityList { entity -> mPlayerId == entity.extra.originalController }
        val cardIdsFromDeck = ArrayList<String>()

        /*
         * build a list of all the ids in mDeck
         */
        for ((key, value) in mPlayerCardMap!!) {
            for (i in 0 until value) {
                cardIdsFromDeck.add(key)
            }
        }

        /*
         * remove the ones that have been revealed already
         */
        for (entity in originalDeckEntityList) {
            if (!TextUtils.isEmpty(entity.CardID)) {
                val it = cardIdsFromDeck.iterator()
                while (it.hasNext()) {
                    val next = it.next()
                    if (next == entity.CardID) {
                        it.remove()
                        break
                    }
                }
            }
        }

        /*
         * assign a tmpCard to the cards we still don't know
         */
        var i = 0
        for (entity in originalDeckEntityList) {
            if (entity.card == null) {
                if (i < cardIdsFromDeck.size) {
                    entity.extra.tmpCard = CardUtil.getCard(cardIdsFromDeck[i])
                    i++
                }
            }
        }
    }

    private fun getEntityListInZone(playerId: String?, zone: String): EntityList {
        return mGame!!.getEntityList { entity -> playerId == entity.tags[Entity.KEY_CONTROLLER] && zone == entity.tags[Entity.KEY_ZONE] }
    }


    private fun update() {
        if (mGame == null) {
            playerAdapter.setList(getCardMapList(if (mPlayerCardMap != null) mPlayerCardMap!! else HashMap<String, Int>()))
            opponentAdapter.setList(getCardMapList(HashMap()))
        } else {
            /*
             * all the code below uses tmpCard and tmpIsGift so that it can change them without messing up the internal game state
             */
            val allEntities = mGame!!.getEntityList { entity -> true }
            allEntities.forEach { entity ->
                entity.extra.tmpIsGift = !TextUtils.isEmpty(entity.extra.createdBy)
                if (entity.card != null) {
                    entity.extra.tmpCard = entity.card
                } else {
                    entity.extra.tmpCard = CardUtil.UNKNOWN
                }
            }

            updatePlayer()
            updateOpponent()
        }
    }

    private fun updateOpponent() {
        val list = ArrayList<Any>()

        val secrets = getSecrets()
        if (secrets.size > 0) {
            list.add(HeaderItem(Utils.getString(R.string.secrets)))
            list.addAll(secrets)
        }
        list.addAll(hand)

        list.add(HeaderItem(Utils.getString(R.string.allCards)))

        // trying a definition that's a bit different from the player definition here
        val allEntities = mGame!!.getEntityList { e ->
            (mOpponentId == e.tags[Entity.KEY_CONTROLLER]
                    && Entity.ZONE_SETASIDE != e.tags[Entity.KEY_ZONE]
                    && Type.ENCHANTMENT != e.tags[Entity.KEY_CARDTYPE]
                    && Type.HERO != e.tags[Entity.KEY_CARDTYPE]
                    && Type.HERO_POWER != e.tags[Entity.KEY_CARDTYPE]
                    && "PLAYER" != e.tags[Entity.KEY_CARDTYPE])
        }

        list.addAll(entityListToItemList(allEntities, { e -> true }))

        opponentAdapter.setList(list)
    }

    private fun updatePlayer() {
        val list = ArrayList<Any>()

        list.add(HeaderItem(Utils.getString(R.string.deck)))

        if (mPlayerCardMap != null) {
            assignCardsFromDeck()
        }

        val entityList = mGame!!.getEntityList { entity -> mPlayerId == entity.extra.originalController }
        /*
         * Add all the gifts
         * XXX it's not enough to filter on !TextUtils.isEmpty(createdBy)
         * because then we get all enchantments
         * if a gift is in the graveyard, it won't be shown but I guess that's ok
         */
        entityList.addAll(mGame!!.getEntityList { entity ->
            (Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]
                    && mPlayerId != entity.extra.originalController
                    && mPlayerId == entity.tags[Entity.KEY_CONTROLLER])
        })

        list.addAll(entityListToItemList(entityList, { entity -> Entity.ZONE_DECK == entity.tags.get(Entity.KEY_ZONE) }))

        playerAdapter.setList(list)
    }


    fun resetGame() {
        mGame = null
        update()
    }

    override fun gameStarted(game: Game) {
        mGame = game
        mPlayerId = mGame!!.player.entity.PlayerID
        mOpponentId = mGame!!.opponent.entity.PlayerID
        update()
    }

    override fun gameOver() {

    }

    override fun somethingChanged() {
        /*
         * we gate the notification so as not to flood the listeners
         */
        mHandler.removeCallbacks(mUpdateRunnable)
        mHandler.postDelayed(mUpdateRunnable, 200)
    }

    companion object {
        private var sController: Controller? = null

        private fun compareNullSafe(a: String?, b: String?): Int {
            return if (a == null) {
                if (b == null) 0 else 1
            } else {
                if (b == null) -1 else a.compareTo(b)
            }
        }

        private fun getCardMapList(cardMap: HashMap<String, Int>): ArrayList<Any> {
            val list = ArrayList<Any>()
            var unknown = Deck.MAX_CARDS

            for ((key, value) in cardMap) {
                val deckEntry = DeckEntryItem()
                deckEntry.card = CardUtil.getCard(key)
                deckEntry.count = value
                list.add(deckEntry)
                unknown -= deckEntry.count
            }

            Collections.sort(list) { a, b ->
                val da = a as DeckEntryItem
                val db = b as DeckEntryItem

                Utils.compareNullSafe(da.card.cost, db.card.cost)
            }

            if (unknown > 0) {
                list.add(ArcaneTrackerApplication.context.getString(R.string.unknown_cards, unknown))
            }
            return list
        }


        fun get(): Controller {
            if (sController == null) {
                sController = Controller()
            }

            return sController!!
        }

        fun resetAll() {
            get().resetGame()
        }
    }
}
