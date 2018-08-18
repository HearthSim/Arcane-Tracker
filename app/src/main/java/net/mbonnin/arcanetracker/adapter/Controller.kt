package net.mbonnin.arcanetracker.adapter


import android.os.Handler
import android.text.TextUtils
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.hsmodel.enum.PlayerClass
import net.mbonnin.hsmodel.enum.Rarity
import net.mbonnin.hsmodel.enum.Type
import timber.log.Timber
import java.util.*

class Controller : GameLogic.Listener {

    val playerAdapter: ItemAdapter
    val opponentAdapter: ItemAdapter

    private val mHandler: Handler
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
                val card = if (TextUtils.isEmpty(entity.CardID) || entity.extra.hide) {
                    val builder = StringBuilder()
                    builder.append("#").append(GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn))
                    if (entity.extra.mulliganed) {
                        builder.append(" (M)")
                    }
                    CardUtil.unknown(builder.toString())
                } else {
                    entity.card
                }

                val clone = entity.clone()
                if (entity.extra.hide) {
                    clone.extra.createdBy = null
                }

                val deckEntry = DeckEntryItem(
                        card = card!!,
                        gift = !entity.extra.hide && !entity.extra.createdBy.isNullOrEmpty(),
                        count = 1,
                        entityList = listOf(clone)
                )

                list.add(deckEntry)
            }

            return list
        }

    fun getSecrets(): List<DeckEntryItem> {
        val list = ArrayList<DeckEntryItem>()

        val entities = getEntityListInZone(mOpponentId, Entity.ZONE_SECRET)
                .filter { e -> Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY] }
                .sortedBy { it.tags[Entity.KEY_ZONE_POSITION] }

        for (entity in entities) {
            val card = if (TextUtils.isEmpty(entity.CardID)) {
                val clazz = entity.tags[Entity.KEY_CLASS]

                if (clazz != null) {
                    CardUtil.secret(clazz)
                } else {
                    CardUtil.secret("MAGE")
                }
            } else {
                entity.card
            }

            val clone = entity.clone()
            clone.card = card
            val deckEntry = DeckEntryItem(
                    card = card!!,
                    gift = !entity.extra.createdBy.isNullOrEmpty(),
                    count = 1,
                    entityList = listOf(clone)
            )
            list.add(deckEntry)
        }

        return list
    }

    fun getTestSecrets(): List<DeckEntryItem> {
        val list = ArrayList<DeckEntryItem>()

        val entity = Entity()
        entity.tags[Entity.KEY_ZONE] = Entity.ZONE_SECRET
        entity.tags[Entity.KEY_CLASS] = PlayerClass.MAGE
        val deckEntry = DeckEntryItem(
                card = CardUtil.secret("MAGE"),
                gift = false,
                count = 1,
                entityList = listOf(entity)
        )

        list.add(deckEntry)

        return list
    }


    init {
        opponentAdapter = ItemAdapter()
        playerAdapter = ItemAdapter()

        GameLogic.get().addListener(this)
        mHandler = Handler()
    }

    fun setPlayerCardMap(cardMap: HashMap<String, Int>) {
        mPlayerCardMap = cardMap
        update()
    }

    private fun getEntityListInZone(playerId: String?, zone: String): List<Entity> {
        return mGame!!.getEntityList { entity -> playerId == entity.tags[Entity.KEY_CONTROLLER] && zone == entity.tags[Entity.KEY_ZONE] }
    }


    private fun update() {
        if (mGame == null) {
            if (TestSwitch.SECRET_LAYOUT) {
                playerAdapter.setList(getTestSecrets() as ArrayList<Any>)
            } else {
                playerAdapter.setList(getCardMapList(if (mPlayerCardMap != null) mPlayerCardMap!! else HashMap<String, Int>()))
            }

            val list = getCardMapList(HashMap())
            opponentAdapter.setList(list)
        } else {

            playerAdapter.setList(getPlayerList(mPlayerCardMap ?: emptyMap()))
            opponentAdapter.setList(getOpponentList())
        }
    }

    private fun getOpponentList(): List<Any> {
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

        val sanitizedEntities = sanitizeEntities(allEntities)

        val intermediateList = mutableListOf<Intermediate>()
        var unknownCards = 0
        sanitizedEntities.forEach {
            if (it.CardID == null && it.extra.createdBy.isNullOrEmpty()) {
                unknownCards++
            } else {
                intermediateList.add(Intermediate(it.CardID, it))
            }
        }

        list.addAll(intermediateToDeckEntryList(intermediateList, { true }))
        if (unknownCards > 0){
            list.add(ArcaneTrackerApplication.context.getString(R.string.unknown_cards, unknownCards))
        }
        return list
    }

    private fun sanitizeEntities(entityList: List<Entity>): List<Entity> {
        return entityList.map {
            if (it.extra.hide) {
                //if the card is hidden, we don't want to disclose when it was drawn
                val clone = it.clone()
                clone.extra.drawTurn = -1
                clone
            } else {
                it
            }
        }
    }

    class Intermediate(val cardId: String?, val entity: Entity)

    private fun getPlayerList(cardMap: Map<String, Int>): List<Any> {
        val list = mutableListOf<Any>()

        list.add(HeaderItem(Utils.getString(R.string.deck)))

        val knownIdList = ArrayList<String>()

        val intermediateList = mutableListOf<Intermediate>()
        /*
         * build a list of all the ids that we know from the deck or from whizbang
         */
        for ((key, value) in cardMap) {
            for (i in 0 until value) {
                knownIdList.add(key)
            }
        }

        val originalDeckEntityList = mGame!!.getEntityList { entity -> mPlayerId == entity.extra.originalController }

        /*
         * remove the ones that have been revealed already
         */
        val revealedEntityList = originalDeckEntityList.filter { !it.CardID.isNullOrBlank() }
        for (entity in revealedEntityList) {
            val it = knownIdList.iterator()
            while (it.hasNext()) {
                val next = it.next()
                if (next == entity.CardID) {
                    it.remove()
                    break
                }
            }
        }

        /*
         * add the revealed cards
         */
        intermediateList.addAll(revealedEntityList.map { Intermediate(it.CardID, it) })

        /*
         * add the known cards from the deck, assume they are still inside the deck
         */
        intermediateList.addAll(knownIdList.map { Intermediate(it, Entity.UNKNOWN_ENTITY) })

        /*
         * Add all the gifts
         * XXX it's not enough to filter on !TextUtils.isEmpty(createdBy)
         * because then we get all enchantments
         * if a gift is in the graveyard, it won't be shown but I guess that's ok
         */
        val giftList = mGame!!.getEntityList { entity ->
            Entity.ZONE_DECK == entity.tags[Entity.KEY_ZONE]
                    && mPlayerId != entity.extra.originalController
                    && mPlayerId == entity.tags[Entity.KEY_CONTROLLER]
        }

        intermediateList.addAll(giftList.map { Intermediate(it.CardID, it) })

        list.addAll(intermediateToDeckEntryList(intermediateList, { it == Entity.UNKNOWN_ENTITY || it.tags[Entity.KEY_ZONE] == Entity.ZONE_DECK }))

        /*
         * and the unknown if any
         */
        val unknownCards = originalDeckEntityList.size - revealedEntityList.size - knownIdList.size
        if (unknownCards > 0) {
            list.add(ArcaneTrackerApplication.context.getString(R.string.unknown_cards, unknownCards))
        }
        if (unknownCards < 0) {
            Timber.e("too many known card ids: $unknownCards")
        }

        return list
    }

    data class GroupingKey(val cardId: String?, val gift: Boolean)

    private fun intermediateToDeckEntryList(intermediateList: List<Intermediate>, increasesCount: (Entity) -> Boolean): List<DeckEntryItem> {
        val map = intermediateList.groupBy({ GroupingKey(it.cardId, !it.entity.extra.createdBy.isNullOrEmpty()) }, { it.entity })

        val deckEntryList = map.map {
            val cardId = it.key.cardId
            val card = if (cardId == null) {
                CardUtil.UNKNOWN
            } else {
                CardUtil.getCard(cardId)
            }
            val entityList = it.value

            val count = entityList
                    .map { if (increasesCount(it)) 1 else 0 }
                    .sum()

            DeckEntryItem(card,
                    gift = it.key.gift,
                    count = count,
                    entityList = entityList)
        }

        return deckEntryList.sortedBy {
            val costString = it.card.cost?.toString() ?: ""
            val giftString = if(it.gift)  "b" else "a"

            "$costString${it.card.name}$giftString"
        }
    }


    override fun gameStarted(game: Game) {
        mGame = game
        mPlayerId = mGame!!.player!!.entity!!.PlayerID
        mOpponentId = mGame!!.opponent!!.entity!!.PlayerID
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

        fun getCardMapList(cardMap: Map<String, Int>): ArrayList<Any> {
            val list = ArrayList<Any>()
            var unknown = Deck.MAX_CARDS

            for ((key, value) in cardMap) {
                val deckEntry = DeckEntryItem(
                        card = CardUtil.getCard(key),
                        count = value,
                        entityList = emptyList())

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
    }
}
