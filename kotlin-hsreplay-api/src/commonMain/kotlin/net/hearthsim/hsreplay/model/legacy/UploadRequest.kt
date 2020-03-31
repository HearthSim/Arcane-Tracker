package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class UploadRequest(
        val match_start: String,
        val friendly_player: String? = null,
        val build: Int,
        val league_id: Int? = null,
        val game_type: Int,
        val players: List<HSPlayer>?,
        val spectator_mode: Boolean,
        val format: Int
)
