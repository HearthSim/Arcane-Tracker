package net.mbonnin.arcanetracker.parser

import com.annimon.stream.function.Predicate
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.hsmodel.enum.Type
import java.util.*

class EntityList : ArrayList<Entity>() {

    class CardTypePredicate internal constructor(private val mCardType: String) : Predicate<Entity> {
        override fun test(entity: Entity): Boolean {
            return mCardType == entity.tags[Entity.KEY_CARDTYPE]
        }
    }

    class ZonePredicate(private val mZone: String) : Predicate<Entity> {
        override fun test(entity: Entity): Boolean {
            return mZone == entity.tags[Entity.KEY_ZONE]
        }
    }

    class NegatePredicate(private val mPredicate: Predicate<Entity>) : Predicate<Entity> {
        override fun test(entity: Entity): Boolean {
            return !mPredicate.test(entity)
        }
    }


    fun filter(predicate: Predicate<Entity>): EntityList {
        val list = EntityList()
        for (entity in this) {
            if (predicate.test(entity)) {
                list.add(entity)
            }
        }
        return list
    }

    fun toCardMap(): HashMap<String, Int> {
        val map = HashMap<String, Int>()
        for (entity in filter(HAS_CARD_ID)) {
            Utils.cardMapAdd(map, entity.CardID!!, 1)
        }
        return map
    }

    companion object {
        val IS_IN_DECK: Predicate<Entity> = ZonePredicate(Entity.ZONE_DECK)
        val IS_NOT_IN_DECK: Predicate<Entity> = NegatePredicate(IS_IN_DECK)
        val IS_IN_HAND: Predicate<Entity> = ZonePredicate(Entity.ZONE_HAND)

        val HAS_CARD_ID = Predicate<Entity>{ entity -> !Utils.isEmpty(entity.CardID) }
        private val IS_ENCHANTMENT = CardTypePredicate(Type.ENCHANTMENT)
        val IS_NOT_ENCHANTMENT: Predicate<Entity> = NegatePredicate(IS_ENCHANTMENT)
    }
}
