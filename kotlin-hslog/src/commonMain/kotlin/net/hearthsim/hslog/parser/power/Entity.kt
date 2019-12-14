package net.hearthsim.hslog.parser.power

import net.hearthsim.hsmodel.Card

@Suppress("PropertyName")
class Entity {
    var EntityID: String? = null
    var CardID: String? = null // might be null if the entity is not revealed yet
    var PlayerID: String? = null // only valid for player entities

    val tags = mutableMapOf<String, String>()

    /**
     * extra information added by us
     */
    var extra = Extra()
    var card: Card? = null

    override fun toString(): String {
        val name = if (card != null) {
            "(${card!!.name})"
        } else {
            ""
        }
        return "CardEntity [id=$EntityID][CardID=$CardID]$name"
    }

    fun setCardId(cardID: String, card: Card) {
        this.CardID = cardID
        this.card = card
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
        const val KEY_MULLIGAN_STATE = "MULLIGAN_STATE"
        const val KEY_ZONE = "ZONE"
        const val KEY_CONTROLLER = "CONTROLLER"
        const val KEY_CARDTYPE = "CARDTYPE"
        const val KEY_FIRST_PLAYER = "FIRST_PLAYER"
        const val KEY_PLAYSTATE = "PLAYSTATE"
        const val KEY_STEP = "STEP"
        const val KEY_TURN = "TURN"
        const val KEY_ZONE_POSITION = "ZONE_POSITION"
        const val KEY_DEFENDING = "DEFENDING"
        const val KEY_CLASS = "CLASS"
        const val KEY_RARITY = "RARITY"
        const val KEY_CURRENT_PLAYER = "CURRENT_PLAYER"
        const val KEY_NUM_CARDS_PLAYED_THIS_TURN = "NUM_CARDS_PLAYED_THIS_TURN"
        const val KEY_TIMEOUT = "TIMEOUT"
        const val KEY_BACON_DUMMY_PLAYER = "BACON_DUMMY_PLAYER"
        const val KEY_ATK = "ATK"
        const val KEY_HEALTH = "HEALTH"
        const val KEY_POISONOUS = "POISONOUS"
        const val KEY_DIVINE_SHIELD = "DIVINE_SHIELD"
        const val KEY_PLAYER_LEADERBOARD_PLACE = "PLAYER_LEADERBOARD_PLACE"
        const val KEY_SIDEQUEST = "SIDEQUEST"

        const val PLAYSTATE_WON = "WON"

        const val ZONE_DECK = "DECK"
        const val ZONE_HAND = "HAND"
        const val ZONE_PLAY = "PLAY"
        const val ZONE_GRAVEYARD = "GRAVEYARD"
        const val ZONE_SECRET = "SECRET"
        const val ZONE_SETASIDE = "SETASIDE"
        const val STEP_FINAL_GAMEOVER = "FINAL_GAMEOVER"
        const val STEP_BEGIN_MULLIGAN = "BEGIN_MULLIGAN"
        const val STEP_MAIN_READY = "MAIN_READY"

        val UNKNOWN_ENTITY = Entity()
    }

}
