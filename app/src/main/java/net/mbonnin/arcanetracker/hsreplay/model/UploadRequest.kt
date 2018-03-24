package net.mbonnin.arcanetracker.hsreplay.model


class UploadRequest {
    var match_start: String? = null
    var friendly_player: String? = null
    var build: Int = 0
    var game_type: Int = 0
    var player1 = HSPlayer()
    var player2 = HSPlayer()
    var spectator_mode: Boolean = false
    var format: String? = null
}
