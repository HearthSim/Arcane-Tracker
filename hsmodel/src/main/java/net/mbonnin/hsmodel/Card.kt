package net.mbonnin.hsmodel


data class Card(
    @JvmField
    val id: String,
    @JvmField
    val dbfId: Int,
    @JvmField
    val type: String,
    @JvmField
    val playerClass: String,
    @JvmField
    val set: String,
    @JvmField
    val cost: Int,
    @JvmField
    val name: String,
    @JvmField
    val text: String? = null, // this can be null sometimes
    @JvmField
    val rarity: String? = null,
    @JvmField
    val race: String? = null,
    @JvmField
    val attack: Int? = null,
    @JvmField
    val health: Int? = null,
    @JvmField
    val durability: Int? = null,
    @JvmField
    val collectible: Boolean = false) : Comparable<String> {

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
