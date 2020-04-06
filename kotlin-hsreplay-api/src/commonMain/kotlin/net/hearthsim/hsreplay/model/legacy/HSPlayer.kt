package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class HSPlayer(
    val player_id: Int,
    val deck: List<String>? = emptyList(),
    val star_level: Int? = null,
    val deck_id: Long? = null)
