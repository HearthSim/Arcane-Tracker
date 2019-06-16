package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class HSPlayer(var rank: Int? = null, val deck: List<String>, val deck_id: Long? = null)
