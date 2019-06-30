package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class HSPlayer(var rank: Int? = null, val deck: List<String> ?= emptyList(), val deck_id: Long? = null)
