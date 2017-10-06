package net.mbonnin.hsmodel


class Card : Comparable<String> {

    @JvmField
    var name: String? = null
    @JvmField
    var playerClass: String? = null

    lateinit var id: String
    @JvmField
    var rarity: String? = null
    @JvmField
    var type: String? = null
    @JvmField
    var text: String? = null
    @JvmField
    var race: String? = null
    @JvmField
    var set: String? = null
    @JvmField
    var multiClassGroup: String? = null
    @JvmField
    var dbfId: Int = 0

    @JvmField
    var cost: Int? = null
    @JvmField
    var attack: Int? = null
    @JvmField
    var health: Int? = null
    @JvmField
    var durability: Int? = null
    @JvmField
    var collectible: Boolean? = null

    override fun compareTo(other: String): Int {
        return id!!.compareTo(other)
    }

    override fun toString(): String {
        return "$name($id)"
    }

    companion object {

        const val UNKNOWN_COST = -1
        const val UNKNOWN_TYPE = "UNKNOWN_TYPE"

        const val CLASS_INDEX_WARRIOR = 0
        const val CLASS_INDEX_NEUTRAL = 9

    }

    var scores: List<TierScore>? = null

}
