package net.hearthsim.hslog.parser.power

sealed class Tag
data class SpectatorTag(val spectator: Boolean) : Tag()
data class BuildNumberTag(val buildNumber: String) : Tag()
data class GameTypeTag(val gameType: String) : Tag()
data class FormatTypeTag(val formatType: String) : Tag()
data class ScenarioIdTag(val scenarioId: String) : Tag()
data class PlayerMappingTag(val playerId: String, val playerName: String) : Tag()
class BlockTag(
        var BlockType: String,
        var Entity: String?,
        var EffectCardId: String,
        var EffectIndex: String,
        var Target: String?,
        var children: MutableList<Tag>,
        var SubOption: String,
        var TriggerKeyword: String?
) : Tag() {
    companion object {
        val TYPE_PLAY = "PLAY"
        val TYPE_POWER = "POWER"
        val TYPE_TRIGGER = "TRIGGER"
        val TYPE_ATTACK = "ATTACK"
    }
}

class CreateGameTag : Tag() {
    lateinit var gameEntity: GameEntityTag
    val playerList: MutableList<PlayerTag> = mutableListOf()
}

class FullEntityTag : Tag() {
    var ID: String? = null
    var CardID: String? = null
    var tags: MutableMap<String, String> = mutableMapOf()
}

class GameEntityTag : Tag() {
    var EntityID: String? = null
    var tags: MutableMap<String, String> = mutableMapOf()
}

class HideEntityTag : Tag() {
    var Entity: String? = null
    var tag: String? = null
    var value: String? = null
}

class MetaDataTag : Tag() {

    var Meta: String? = null
    var Data: String? = null
    var Info = ArrayList<String>()

    companion object {
        val META_DAMAGE = "DAMAGE"
        val META_TARGET = "TARGET"
    }
}

class PlayerTag : Tag() {
    var EntityID: String? = null
    var PlayerID: String? = null
    var tags: MutableMap<String, String> = mutableMapOf()
}

class ShowEntityTag : Tag() {
    var Entity: String? = null
    var CardID: String? = null
    var tags: MutableMap<String, String> = mutableMapOf()
}

class TagChangeTag : Tag() {
    var ID: String? = null
    var tag: String? = null
    var value: String? = null
}
