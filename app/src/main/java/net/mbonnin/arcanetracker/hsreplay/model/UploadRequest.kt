package net.mbonnin.arcanetracker.hsreplay.model

import net.mbonnin.arcanetracker.BnetGameType
import net.mbonnin.arcanetracker.FormatType


class UploadRequest {
    var match_start: String? = null
    var friendly_player: String? = null
    var build: Int = 0
    var game_type = BnetGameType.BGT_UNKNOWN.intValue
    var player1 = HSPlayer()
    var player2 = HSPlayer()
    var spectator_mode: Boolean = false
    var format = FormatType.FT_UNKNOWN.intValue
}
