package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class UploadRequest (
    val match_start: String,
    val friendly_player: String,
    val build: Int,
    val game_type: Int,
    val player1: HSPlayer,
    val player2: HSPlayer,
    val spectator_mode: Boolean,
    val format: Int
)
