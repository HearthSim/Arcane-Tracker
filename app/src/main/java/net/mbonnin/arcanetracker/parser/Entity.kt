package net.mbonnin.arcanetracker.parser

import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.hsmodel.Card
import java.util.*

class Entity {


    var EntityID: String? = null
    var CardID: String? = null // might be null if the entity is not revealed yet
    var PlayerID: String? = null // only valid for player entities

    var tags: HashMap<String, String> = HashMap()

    /**
     * extra information added by us
     */
    var extra = Extra()
    var card: Card? = null

    override fun toString(): String {
        return String.format(Locale.ENGLISH, "CardEntity [id=%s][CardID=%s]%s", EntityID, CardID, if (card != null) "(" + card!!.name + ")" else "")
    }

    fun setCardId(cardID: String) {
        this.CardID = cardID
        this.card = CardUtil.getCard(cardID)
    }

    class Extra {
        var originalController: String? = null
        var drawTurn = -1
        var playTurn = -1
        var diedTurn = -1
        var mulliganed: Boolean = false
        var createdBy: String? = null
        var hide: Boolean = false

        /*
         * secret detector
         */
        val excludedSecretList = mutableListOf<String>()
    }


    fun clone(): Entity {
        val clone = Entity()
        clone.EntityID = EntityID
        clone.PlayerID = PlayerID
        clone.tags.putAll(tags)
        clone.card = card
        clone.CardID = CardID
        clone.extra.drawTurn = extra.drawTurn
        clone.extra.playTurn = extra.playTurn
        clone.extra.createdBy = extra.createdBy
        clone.extra.mulliganed = extra.mulliganed
        clone.extra.excludedSecretList.addAll(extra.excludedSecretList)

        clone.extra.hide = extra.hide
        return clone
    }

    companion object {
        val KEY_ZONE = "ZONE"
        val KEY_CONTROLLER = "CONTROLLER"
        val KEY_CARDTYPE = "CARDTYPE"
        val KEY_FIRST_PLAYER = "FIRST_PLAYER"
        val KEY_PLAYSTATE = "PLAYSTATE"
        val KEY_STEP = "STEP"
        val KEY_TURN = "TURN"
        val KEY_ZONE_POSITION = "ZONE_POSITION"
        val KEY_DEFENDING = "DEFENDING"
        val KEY_CLASS = "CLASS"
        val KEY_RARITY = "RARITY"
        val KEY_CURRENT_PLAYER = "CURRENT_PLAYER"
        val KEY_NUM_CARDS_PLAYED_THIS_TURN = "NUM_CARDS_PLAYED_THIS_TURN"

        val PLAYSTATE_WON = "WON"

        val ZONE_DECK = "DECK"
        val ZONE_HAND = "HAND"
        val ZONE_PLAY = "PLAY"
        val ZONE_GRAVEYARD = "GRAVEYARD"
        val ZONE_SECRET = "SECRET"
        val ZONE_SETASIDE = "SETASIDE"

        val ENTITY_ID_GAME = "1"

        val STEP_FINAL_GAMEOVER = "FINAL_GAMEOVER"
        val STEP_BEGIN_MULLIGAN = "BEGIN_MULLIGAN"

        val UNKNOWN_ENTITY = Entity()
        val KEY_MULLIGAN_STATE = "MULLIGAN_STATE"
        val KEY_TIMEOUT = "TIMEOUT"
    }

}
