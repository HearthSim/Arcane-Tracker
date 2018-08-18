package net.mbonnin.arcanetracker.parser.power

import java.util.ArrayList

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
