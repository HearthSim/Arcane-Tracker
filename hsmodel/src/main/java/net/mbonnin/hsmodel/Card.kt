package net.mbonnin.hsmodel


data class Card(
    @JvmField
    val name: String? = null,
    @JvmField
    val playerClass: String? = null,
    @JvmField
    val id: String,
    @JvmField
    val rarity: String? = null,
    @JvmField
    val type: String? = null,
    @JvmField
    val text: String? = null,
    @JvmField
    val race: String? = null,
    @JvmField
    val set: String? = null,
    @JvmField
    val multiClassGroup: String? = null,
    @JvmField
    val dbfId: Int = 0,
    @JvmField
    val cost: Int? = null,
    @JvmField
    val attack: Int? = null,
    @JvmField
    val health: Int? = null,
    @JvmField
    val durability: Int? = null,
    @JvmField
    val collectible: Boolean? = null) : Comparable<String> {

    override fun compareTo(other: String): Int {
        return id.compareTo(other)
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
