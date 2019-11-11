package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class UploadRequest(
        val match_start: String,
        val friendly_player: String? = null,
        val build: Int,
        val game_type: Int,
        val player1: HSPlayer? = null,
        val player2: HSPlayer? = null,
        val spectator_mode: Boolean,
        val format: Int
)
