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
        val name: String,
        @JvmField
        val cost: Int? = null, // null for hero cards & enchantments
        @JvmField
        val text: String? = null, //  null for hero cards, also for some regular cards like snowflipper penguin
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
        val multiClassGroup: String? = null,
        @JvmField
        val collectible: Boolean = false,
        val features: DoubleArray? = null,
        val goldenFeatures: DoubleArray? = null,
        val scores: List<TierScore>? = null
) : Comparable<String> {


    override fun compareTo(other: String): Int {
        return id.compareTo(other)
    }

    override fun toString(): String {
        return "$name($id)"
    }

    fun isStandard(): Boolean {
        return setOf(Set.CORE, Set.EXPERT1, Set.OG, Set.KARA, Set.GANGS, Set.UNGORO, Set.ICECROWN, Set.LOOTAPALOOZA).contains(set)
    }


    // https://us.battle.net/forums/en/hearthstone/topic/20758787441
    fun isDraftable(): Boolean {
        return isStandard()
                && !(type == Type.HERO) // DK heroes are not available
                && !(UNDRAFTABLE_CARDS.contains(id))
    }

    companion object {

        val UNKNOWN_COST: Int? = null
        const val UNKNOWN_TYPE = "UNKNOWN_TYPE"

        const val CLASS_INDEX_WARRIOR = 0
        const val CLASS_INDEX_NEUTRAL = 9

        val UNDRAFTABLE_CARDS = setOf(CardId.VICIOUS_FLEDGLING,
                // quests
                CardId.JUNGLE_GIANTS,
                CardId.THE_MARSH_QUEEN,
                CardId.OPEN_THE_WAYGATE,
                CardId.THE_LAST_KALEIDOSAUR,
                CardId.AWAKEN_THE_MAKERS,
                CardId.THE_CAVERNS_BELOW,
                CardId.UNITE_THE_MURLOCS,
                CardId.LAKKARI_SACRIFICE,
                CardId.FIRE_PLUMES_HEART,
                // cthun
                CardId.KLAXXI_AMBERWEAVER,
                CardId.DARK_ARAKKOA,
                CardId.CULT_SORCERER,
                CardId.HOODED_ACOLYTE,
                CardId.TWILIGHT_DARKMENDER,
                CardId.BLADE_OF_CTHUN,
                CardId.USHER_OF_SOULS,
                CardId.ANCIENT_SHIELDBEARER,
                CardId.TWILIGHT_GEOMANCER,
                CardId.DISCIPLE_OF_CTHUN,
                CardId.TWILIGHT_ELDER,
                CardId.CTHUNS_CHOSEN,
                CardId.CRAZED_WORSHIPPER,
                CardId.SKERAM_CULTIST,
                CardId.TWIN_EMPEROR_VEKLOR,
                CardId.DOOMCALLER,
                CardId.CTHUN,
                // other
                CardId.DUST_DEVIL,
                CardId.TOTEMIC_MIGHT,
                CardId.ANCESTRAL_HEALING,
                CardId.WINDSPEAKER,
                CardId.SACRIFICIAL_PACT,
                CardId.SENSE_DEMONS,
                CardId.FACELESS_SUMMONER,
                CardId.SUCCUBUS,
                CardId.SAVAGERY,
                CardId.SOUL_OF_THE_FOREST,
                CardId.MARK_OF_NATURE,
                CardId.WARSONG_COMMANDER,
                CardId.RAMPAGE,
                CardId.STARVING_BUZZARD,
                CardId.TIMBER_WOLF,
                CardId.SNIPE,
                CardId.MIND_BLAST,
                CardId.LIGHTWELL,
                CardId.PURIFY,
                CardId.INNER_FIRE
        )
    }
}
