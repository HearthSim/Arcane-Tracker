package net.mbonnin.arcanetracker.parser.power

data class SpectatorTag(val spectator: Boolean): Tag()
data class BuildNumberTag(val buildNumber: String): Tag()
data class GameTypeTag(val gameType: String): Tag()
data class FormatTypeTag(val formatType: String): Tag()
data class ScenarioIdTag(val scenarioId: String): Tag()
data class PlayerMappingTag(val playerId: String, val playerName: String): Tag()