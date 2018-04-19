package net.mbonnin.arcanetracker.parser

import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.hsmodel.Card
import timber.log.Timber
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
        /**
         * used from Controller.java to affect a temporary id to cards we don't know yet
         */
        var tmpCard: Card? = null
        var tmpIsGift: Boolean = false

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
        val possibleSecretList = mutableListOf<String>()

        var otherPlayerPlayedMinion: Boolean = false
        var otherPlayerCastSpell: Boolean = false
        var otherPlayerHeroPowered: Boolean = false
        var selfHeroAttacked: Boolean = false
        var selfMinionWasAttacked: Boolean = false
        var selfHeroDamaged: Boolean = false
        var selfPlayerMinionDied: Boolean = false
        var selfMinionTargetedBySpell: Boolean = false
        var competitiveSpiritTriggerConditionHappened: Boolean = false
        var otherPlayerPlayedMinionWithThreeOnBoardAlready: Boolean = false
        var selfHeroAttackedByMinion: Boolean = false
    }

    fun dump() {
        for (key in tags.keys) {
            Timber.v("   " + key + "=" + tags[key])
        }
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

        clone.extra.otherPlayerPlayedMinion = extra.otherPlayerPlayedMinion
        clone.extra.otherPlayerCastSpell = extra.otherPlayerCastSpell
        clone.extra.otherPlayerHeroPowered = extra.otherPlayerHeroPowered
        clone.extra.selfHeroAttacked = extra.selfHeroAttacked
        clone.extra.selfMinionWasAttacked = extra.selfMinionWasAttacked
        clone.extra.selfHeroDamaged = extra.selfHeroDamaged
        clone.extra.selfPlayerMinionDied = extra.selfPlayerMinionDied
        clone.extra.selfMinionTargetedBySpell = extra.selfMinionTargetedBySpell
        clone.extra.competitiveSpiritTriggerConditionHappened = extra.competitiveSpiritTriggerConditionHappened
        clone.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready = extra.otherPlayerPlayedMinionWithThreeOnBoardAlready
        clone.extra.selfHeroAttackedByMinion = extra.selfHeroAttackedByMinion
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
    }

}
