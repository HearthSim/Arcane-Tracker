package net.mbonnin.arcanetracker.ui.overlay.adapter


import android.os.Handler
import android.text.TextUtils
import net.mbonnin.arcanetracker.*
import net.hearthsim.hslog.Deck
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.GameLogic
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.PlayerClass
import net.hearthsim.hsmodel.enum.Rarity
import net.hearthsim.hsmodel.enum.Type
import timber.log.Timber
import java.util.*

class Controller  {

    val playerAdapter: ItemAdapter
    val opponentAdapter: ItemAdapter

    private val mHandler: Handler
    protected var mPlayerCardMap: Map<String, Int>? = null
    private var mGame: Game? = null
    private var mPlayerId: String? = null
    private var mOpponentId: String? = null

    private val mUpdateRunnable = Runnable { this.update() }

    private fun opponentHand(): List<DeckEntryItem> {
        val list = ArrayList<DeckEntryItem>()

        val entities = getEntityListInZone(mOpponentId, Entity.ZONE_HAND)
                .sortedBy { it.tags[Entity.KEY_ZONE_POSITION] }

        for (entity in entities) {
            val card = entity.card
            val deckEntry = if (card == null || entity.extra.hide) {
                val builder = StringBuilder()
                builder.append("#").append(GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn))
                if (entity.extra.mulliganed) {
                    builder.append(" (M)")
                }
                val drawTurn = GameLogic.gameTurnToHumanTurn(entity.extra.drawTurn)
                val mulliganed = if (entity.extra.mulliganed) " (M)" else ""

                val displayedEntity = Entity()
                displayedEntity.extra.drawTurn = entity.extra.drawTurn

                DeckEntryItem(
                        card = CardJson.unknown("#${drawTurn}${mulliganed}"),
                        gift = false,
                        count = 1,
                        entityList = listOf(displayedEntity)
                )
            } else {
                DeckEntryItem(
                        card = card,
                        gift = !entity.extra.createdBy.isNullOrEmpty(),
                        count = 1,
                        entityList = listOf(entity)
                )
            }

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

        for (i in 0 until 3) {
            for (playerClass in listOf(PlayerClass.MAGE, PlayerClass.HUNTER, PlayerClass.PALADIN, PlayerClass.ROGUE)) {
                val entity = Entity()
                entity.tags[Entity.KEY_ZONE] = Entity.ZONE_SECRET
                entity.tags[Entity.KEY_CLASS] = playerClass
                entity.extra.drawTurn = 1
                entity.extra.mulliganed = Math.random() < 0.5
                entity.extra.createdBy = "toto"
                val deckEntry = DeckEntryItem(
                        card = CardUtil.secret(playerClass),
                        gift = false,
                        count = 1,
                        entityList = listOf(entity)
                )
                list.add(deckEntry)
            }
        }

        return list
    }

    init {
        opponentAdapter = ItemAdapter()
        playerAdapter = ItemAdapter()

        mHandler = Handler()
    }

    fun setPlayerCardMap(cardMap: Map<String, Int>) {
        mPlayerCardMap = cardMap
        update()
    }

    private fun getEntityListInZone(playerId: String?, zone: String): List<Entity> {
        return mGame!!.getEntityList { entity -> playerId == entity.tags[Entity.KEY_CONTROLLER] && zone == entity.tags[Entity.KEY_ZONE] }
    }


    private fun update() {
        if (mGame == null) {
            val list = mutableListOf<Any>()
            if (TestSwitch.SECRET_LAYOUT) {
                list.addAll(getTestSecrets())
            }
            list.addAll(getCardMapList(mPlayerCardMap ?: emptyMap()))
            playerAdapter.setList(list)

            opponentAdapter.setList(getCardMapList(HashMap()))
        } else {

            playerAdapter.setList(getPlayerList(mPlayerCardMap ?: emptyMap()))
            opponentAdapter.setList(getOpponentList())
        }
    }

    private fun getOpponentList(): List<Any> {
        val list = ArrayList<Any>()

        val secrets = getSecrets()
        if (secrets.isNotEmpty()) {
            list.add(HeaderItem(Utils.getString(R.string.secrets)))
            list.addAll(secrets)
        }

        val handDeckEntryItemList = opponentHand()
        list.add(HeaderItem(Utils.getString(R.string.hand) + " (" + handDeckEntryItemList.size + ")"))
        list.addAll(handDeckEntryItemList)

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

        // the logic is a bit different than in opponentHand(). Here we want to display when the card
        // was draw (think prince maltezaar)
        val sanitizedEntities = allEntities.map {
            if (it.extra.hide) {
                val displayedEntity = Entity()
                displayedEntity.CardID = it.CardID
                displayedEntity.card = it.card
                displayedEntity
            } else {
                it
            }
        }

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
        if (unknownCards > 0) {
            list.add(ArcaneTrackerApplication.context.getString(R.string.unknown_cards, unknownCards))
        }
        return list
    }


    class Intermediate(val cardId: String?, val entity: Entity)

    private fun getPlayerList(cardMap: Map<String, Int>): List<Any> {
        val list = mutableListOf<Any>()

        list.add(HeaderItem(Utils.getString(R.string.deck)))

        val originalDeckEntityList = mGame!!.getEntityList { entity -> mPlayerId == entity.extra.originalController }

        val knownIdList = ArrayList<String>()

        /*
         * build a list of all the ids that we know from the deck or from whizbang
         */
        for ((key, value) in cardMap) {
            for (i in 0 until value) {
                knownIdList.add(key)
            }
        }

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
        val intermediateList = mutableListOf<Intermediate>()
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
                CardJson.UNKNOWN
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

        return deckEntryList.sortedWith(deckEntryComparator)
    }



    fun gameStarted(game: Game) {
        mGame = game
        mPlayerId = mGame!!.player!!.entity!!.PlayerID
        mOpponentId = mGame!!.opponent!!.entity!!.PlayerID

        update()
    }

    fun somethingChanged() {
        /*
         * we gate the notification so as not to flood the listeners
         */
        mHandler.removeCallbacks(mUpdateRunnable)
        mHandler.postDelayed(mUpdateRunnable, 200)
    }

    companion object {
        private var sController: Controller? = null

        val deckEntryComparator = kotlin.Comparator<DeckEntryItem>{ a, b ->
            val acost = a.card.cost
            val bcost = b.card.cost

            if (acost == null && bcost != null) {
                return@Comparator -1
            } else if (acost != null && bcost == null) {
                return@Comparator 1
            } else if (acost != null && bcost != null) {
                val r = acost - bcost
                if (r != 0) {
                    return@Comparator r
                }
            }

            val r = a.card.name.compareTo(b.card.name)
            if (r != 0) {
                return@Comparator r
            }

            val agift = if (a.gift) 1 else 0
            val bgift = if (b.gift) 1 else 0
            return@Comparator agift - bgift
        }

        fun getCardMapList(cardMap: Map<String, Int>): List<Any> {
            val deckEntryList = mutableListOf<DeckEntryItem>()
            val list = mutableListOf<Any>()
            var unknown = Deck.MAX_CARDS

            for ((key, value) in cardMap) {
                val deckEntry = DeckEntryItem(
                        card = CardUtil.getCard(key),
                        count = value,
                        entityList = emptyList())

                deckEntryList.add(deckEntry)
                unknown -= deckEntry.count
            }

            list.addAll(deckEntryList.sortedWith(deckEntryComparator))

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
