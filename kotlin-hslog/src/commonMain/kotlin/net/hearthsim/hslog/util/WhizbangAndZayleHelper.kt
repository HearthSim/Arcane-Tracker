package net.hearthsim.hslog.util

import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.decks.DeckStringHelper
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson

object WhizbangAndZayleHelper {
    val whizbangRecipes = listOf(
            "AAECAZICBNWDA8OUA9OcA/etAw39Au0D9wPmBdfvAt/7Ar/9ArSJA86UA6+iA/ytA/6tA/+tAwA=", //Trees are friends
            "AAECAZICBvX8AuyJA+iUA/atA4iwA4exAwxAX8QGjQiJ8QK/8gLoiQOvogOwrQP9rQOMrgOBsQMA", //Ysera dreams
            "AAECAR8GuwXtCcn4AuOLA+aWA6SlAwy1A94E2wmBCvbsAqCFA6SIA9ePA56dA8edA+SkA5ipAwA=", //Unseal the vault
            "AAECAR8KxwOvBMkErgaY8AKA8wKnggOghQOfpQOHsAMKngG1A4cE/gz1iQO+mAOipQP7rwP8rwOFsAMA", // Dragonbane devastation
            "AAECAf0EHk2KAbsCiwPJA6sEywSWBd4F8gWKB+wH+wye8AK38QLG+AKggAPsiQPnlQO9mQOfmwOKngOhoQP8owOSpAO/pAOEpwP1rAPsrwORsQMAAA==", // Reno's riches
            "AAECAf0EBJaaA4qeA4GxA+G2Aw3hB40Izu8CifEC6IkD7IkD55UDg5YDoJsD9awD+qwD8K8Dh7EDAA==", // Aspect of the arcane
            "AAECAZ8FHooB3APSBN4F8gX0Bc8G+gavB/YH/gePCfsMm/AC/fsChPwCoIADvYYD44YD7IYD7IkDg6EDoaED/KMDw6QDhKcDlqwDiK4DkK4DkbEDAAA=", // Perfect justice
            "AAECAZ8FAvoG/fsCDtwDrwf+B9b+Atn+AsWhA8OkA5amA5asA4ytA4euA5CuA5uuA5yuAwA=", // Lightforge retribution
            "AAECAa0GCNcKvfMC+/4CoIAD1pkDk5sDg6ADn6kDC/gC5QTRCtMK8vECl4cDgpQDmJsDmZsD0qUD2awDAA==", // Day of the dead
            "AAECAa0GCPYH5fcCg5QDqaUDmakDqq8D47QDyMADC+UE1QjSCvIMl4cD0qUDhKgD16wD2qwD/q4Dza8DAA==", // Galakrond wit
            "AAECAaIHBrICmwXr8AK0hgOnqAOqqAMMtAGIB90IhgnH+AKPlwOQlwP7mgP+mgO7pQOtqAO3rgMA", // Bazaar burglary
            "AAECAaIHCJwCsgKvBPYEtIYDkpcD/q4Dy8ADC7QBlwaIB90Ihgm0kQOPlwO2rgO5rgOqrwPOrwMA", // Galakrond guile
            "AAECAaoICqbwAu/3Aur6ArmZA72ZA8WZA9qdA4SnA+GoA/CtAwr1BN4F/gWyBu/xAq2RA7SRA8aZA7ulA8+lAwA=", // Corrupt the waters
            "AAECAaoIBpMJ7/cC9ooD5qUD47QD08ADDI/7Apz/ArSXA8aZA8+lA9SlA7WtA7atA7etA7mtA6qvA9CvAwA=", // Galakrond's fury
            "AAECAf0GAA8w0wHOB9kHsQjCCJDuAp7xAvT3AquRA7+YA4CaA4edA4idA/qkAwA=", // Ultimate Impfestation
            "AAECAf0GBsQInPgCoIAD7IkD8KwD8awDDIoB4QeNCPMM8fcC2pYDoaED+qQD5awD66wD7KwD7qwDAA==", // Clutches of Valdris
            "AAECAQcES6CAA/eoA+iwAw3/A6IE/wf7DJ3wApvzAvT1Ap77ArP8AoiHA5+hA/WoA/aoAwA=", // Hack the system
            "AAECAQcEyAOvBN6tA+iwAw0csAL/A6gF1AXuBucHnfACn6ED9agD3K0D3a0D6bADAA==" // Drop the ancharrr
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