package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.hsmodel.Rarity

object SecretLogic {
    private fun secretEntityList(game: Game): List<Entity> {
        return game.getEntityList { shouldTrack(it) }
    }

    private fun shouldTrack(e: Entity): Boolean {
        return Entity.ZONE_SECRET == e.tags[Entity.KEY_ZONE]
                && Rarity.LEGENDARY != e.tags[Entity.KEY_RARITY]

    }

    fun secretPlayed(entity: Entity) {

    }

    fun cardPlayed(game: Game, playedEntity: Entity) {

    }
}