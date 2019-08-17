package net.hearthsim.hslog.util

import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.decks.DeckStringHelper
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson

object WhizbangAndZayleHelper {
    val whizbangRecipes = listOf(
            "AAECAZICBpvwAvX8AqCAA9OUA8mcA9ulAwxAVpMExAaYB+QI6PwCuZQDypwDu58Dr6IDyKIDAA==", //Nature's Restoration
            "AAECAZICBiRfrtIC9fwCyZwD+KEDDEBWigH3A8QGi+4C4fsCypwDr6IDyKID3KID2akDAA==", //Untapped potential
            "AAECAR8Gh/sCoIADm4UD8ZYD+ZYDn7cDDLUDlwjg9QLi9QLv9QLw9QK09gK5+AKY+wL2/QLylgO2nAMA", //Scrap Heap
            "AAECAR8GuwXtCcn4AuOLA+aWA6SlAwy1A94E2wmBCvbsAqCFA6SIA9ePA56dA8edA+SkA5ipAwA=", // Unseal the vault
            "AAECAf0EBO0FuAju9gKJlgMNuwKrBLQE5gSWBZX/Arn/AqOHA8iHA5+bA+KbA/+dA4ipAwA=", // Archmages of Dalaran
            "AAECAf0EHk2KAbsCiwPJA6sEywSWBd4F8gWKB+wH+wzL7AKe8AK38QLF8wLG+AKggAOvhwPsiQPnlQO9mQOfmwOKngOhoQP8owOSpAO/pAOEpwMAAA==", // Reno's riches
            "AAECAZ8FBowBrwTIBMD9AqeCA5SaAwyeAc8G7gavB63yAtj+AvWJA/mTA76YA46aA5CaA8OkAwA=", // Sunreaver's secrets
            "AAECAZ8FHooB3APSBN4F8gX0Bc8G+gaKB68H9gf+B48J+wz57AKb8AL9+wKE/ALd/gKggAO9hgPjhgPshgPsiQODoQOhoQP8owPDpAOEpwOWrAMAAA==", // Perfect Justice
            "AAECAa0GCNcKvfMC+/4CoIAD1pkDk5sDg6ADn6kDC/gC5QTRCtMK8vECl4cD/okDgpQDmJsDmZsD0qUDAA==", // Day of the dead
            "AAECAa0GBqCAA4OUA6mlA7ulA9OlA92rAwz4AuUE9gfVCNEK0gryDPsMvfMC5fcC0qUDhKgDAA==", // Activate the obelisk
            "AAECAaIHBrICyAPdCKbvAtaMA9uMAwy0AagF1AWIB+cHhgnf7wKq/wLVjAOPlwOQlwP/pQMA", // Cursed crew
            "AAECAaIHBLIC6/ACtIYDp6gDDbQBmwWIB90IhgnH+AKPlwOQlwP7mgP+mgO7pQOqqAOtqAMA", // Bazaar Burglary
            "AAECAaoICqbwAu/3AqH4Aur6ArmZA72ZA8WZA9qdA4SnA+GoAwr1BN4F/gWyBu/xAq2RA7SRA8aZA7ulA8+lAwA=", // Corrupt the waters
            "AAECAaoIAt6CA5ybAw7FA9sD/gPjBdAHpwiTCeKJA4yUA7WYA8aZA/SZA6+nA8qrAwA=", // Underbelly underlings
            "AAECAf0GAA8w0wHOB9kHsQjCCJDuAp7xAvT3AquRA7+YA4CaA4edA4idA/qkAwA=", // Ultimate Impfestation
            "AAECAf0GCtsGxAjMCMLxApz4AqCAA4+CA5eXA4mdA+ujAwowtgfzDMXzAtqWA8KZA9qbA6GhA7ulA9KlAwA=", // Supreme Archeology
            "AAECAQcK0gL8BLj2ApL4AoP7AqCAA5qHA5uKA/aWA9+pAwpLogSRBv8Hsgjy8QKb8wKO+wLYjAOWlAMA", // Dropping the Boom
            "AAECAQcES6CAA/KoA/eoAw3/A6IE/wf7DJ3wApvzAvT1Ap77ArP8AoiHA5+hA/WoA/aoAwA=" // Hack the system
    )

    val zayleRecipes = listOf(
            "AAECAa0GBO0B+ALQ/gKgrAMN3QTlBPYH1QilCdEK0gryDIOUA4eVA5ibA66bA4KdAwA=", // silence priest
            "AAECAaIHCLICzQPtBef6AqCAA7SGA5KXA9KZAwu0Ae0CmwWIB90Ihgmm7wLOjAO0kQOPlwOQlwMA", //tempo rogue
            "AAECAaoICrIGp+4C7/cCmfsCoIADwYkD0pgDuZkDxZkDhp0DCoEE9QTeBf4FrZEDtJEDipQDlZQDtJcDxpkDAA==", //control shaman
            "AAECAf0GCtsG8wzC8QKc+ALN/AKggAOPggPShgOXlwOJnQMKkge2B8QIzAjF8wK09gLalgPCmQPamwODoAMA", // plot twist warlock
            "AAECAQcKuuwCze8Cm/ACkvgCjvsCoIADmocDm5QDkpgDwJgDCp3wApfzAtH1Ap77ArP8AvH8AvWAA5eUA5qUA4OgAwA=" // bomb rush warrior
    )

    fun findWhizbangDeck(game: Game, cardJson: CardJson): Deck? {
        return findDeck(game, whizbangRecipes, cardJson)
    }

    fun finZayleDeck(game: Game, cardJson: CardJson): Deck? {
        return findDeck(game, zayleRecipes, cardJson)
    }

    private fun findDeck(game: Game, recipeCandidateList: List<String>, cardJson: CardJson): Deck? {
        val playerEntityList = game.getEntityList { entity ->
            game.player!!.entity!!.PlayerID == entity.extra.originalController
                    && entity.card != null
        }

        val deck = recipeCandidateList
                .asSequence()
                .map { DeckStringHelper.parse(it, cardJson) }
                .filterNotNull()
                .firstOrNull { deck2 ->
                    playerEntityList.filter { !deck2.cards.containsKey(it.card!!.id) }.isEmpty()
                }

        return deck
    }
}