package net.hearthsim.hslog.util

import net.hearthsim.hslog.Deck
import net.hearthsim.hslog.parser.decks.DeckStringHelper
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson

object WhizbangAndZayleHelper {
    val whizbangRecipes = listOf(
            "AAEBAZICBCT9AsX9AsOUAw3tA/cD5gWxCNfvAt/7AuH7Ar/9AtWDA7SJA86UA8qcA9OcAwA=", // Trees are friends
            "AAEBAZICBu0Fm/AC9fwCoIAD05QDypwDDEBWX5MExAaYB9UI8wz6/gK5lAPPlAO7nwMA", // Nature restauration
            "AAEBAR8CgPMC44sDDqgCtQPeBNkH6wfbCe0JgQqghQOiigOwiwPkiwOenQPHnQMA", // Animal Instincts
            "AAEBAR8EoIADm4UD8ZYD+ZYDDbUDlwjg9QLi9QLv9QK09gK5+AKH+wKY+wL2/QKghQPylgO2nAMA", // The scrap heap
            "AAEBAf0ECMUE+wyggAOvhwPnlQODlgOWmgOKngMLTYoBuwLJA6sEywSWBewHw/gCn5sDoJsDAA==", // Khadgars Creations
            "AAEBAf0EBO0FuAj77ALu9gINuwKrBLQE5gSWBZX/Arn/AqOHA8iHA4mWA5+bA+KbA/+dAwA=", // Archmages of Dalaran
            "AAEBAZ8FCtIE+wyb8AL9+wKE/AKggAO9hgPehgPjhgPOhwMK3AP0Ba8H9gf57ALmhgPshgPvhgOKmgO0mwMA", // Shirvallah's chosen
            "AAEBAZ8FAq8ElJoDDkaMAZ4ByAT1Bc8GrwexCK3yAtj+AvWJA76YA46aA5CaAwA=", // Sunreaver's secret
            "AAEBAa0GBqUJ5fcCoIADi4oDgpQDoKwDDO0B5QTSCtMK8gzc9QK09gKh/gLriAPvkgODlAONlwMA", // Whispers from the grave
            "AAEBAa0GBND+AqCAA5ObA4OgAw3tAeUE1QjRCtIK1wryDPLxAvv+ApeHA4OUA5ibA5mbAwA=", // Day of the dead
            "AAEBAaIHBrICyAPdCM6MA9aMA9uMAwy0AZsFqAXUBYgH5weGCabvAt/vAqr/As+JA5CXAwA=", // Cursed crew
            "AAEBAaIHCLIC6/AC7/MCqPcC5/oCzowDr5EDiJsDC7QBmwX1BYgH3QiGCcf4ApCXA6iYA/uaA/6aAwA=", // The Heist
            "AAEBAaoIBCCiCafuAqCAAw2UA/UE/gWyBp/9Apv/AoqFA72FA/OKA62RA4qUA4+UA8SZAwA=", // Swamp of horrors
            "AAEBAaoIArWYA5ybAw6/AcUD2wP+A+MF0AenCJMJ8PMC3oID4okDjJQDxpkD9JkDAA==", // Underbelly Underlings
            "AAEBAf0GCJME8gXbBooHkgec+AKggAOXlwMLigH7BrYH4QeNCMQIzAjzDMXzAo+AA8KZAwA=", // Hand of Gul'dan
            "AAEBAf0GAA8w0wHVA84H2QexCMIIkO4CnvEC9PcCw/0Cq5EDv5gDgJoDh50DAA==", // Ultimate Impfestation
            "AAEBAQcIkAf/B6IJ+wzeggP4hgOShwO9mQMLogSJ8QKb8wL09QKBhwOLhwPoiQPsiQOqiwPolAPCmQMA", // Wings of war
            "AAEBAQcI0gL8BLj2ApL4AqCAA5qHA5uKA/aWAwtLogSRBv8Hsgjy8QKb8wKO+wLYjAOWlAOZlAMA" // Dropping The Boom
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