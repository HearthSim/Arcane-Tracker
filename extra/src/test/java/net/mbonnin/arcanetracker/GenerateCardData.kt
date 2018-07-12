package net.mbonnin.arcanetracker

import com.squareup.moshi.Types
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardTranslation
import org.junit.Test
import java.io.File


class GenerateCardData {
    @Test
    fun deploy() {
        val cardDataList = mutableListOf<Card>()
        val cardTranslationMap = mutableMapOf<String, MutableMap<String, CardTranslation>>()

        val hsCardList = hsCardList()

        // sanity checks
        hsCardList.forEach {
            if (it.dbfId == null) {
                System.out.println("no dbfId $it")
            }
            if (it.type == null) {
                System.out.println("no type for $it")
            }
            if (it.name == null) {
                System.out.println("no name for $it")
            }
            if (it.cardClass == null) {
                System.out.println("no playerClass for $it")
            }
            if (it.set == null) {
                System.out.println("no set for ${it.name}")
            }
        }

        hsCardList.forEach {
            cardDataList.add(Card(id = it.id,
                    name = "",
                    text = "",
                    playerClass = it.cardClass!!,
                    rarity =  it.rarity,
                    race = it.race,
                    type = it.type!!,
                    set = it.set ?: "CORE",
                    dbfId = it.dbfId!!,
                    cost = it.cost,
                    attack = it.attack,
                    health = it.health,
                    mechanics = it.mechanics?.toSet() ?: emptySet(),
                    durability = it.durability,
                    collectible = it.collectible?:false,
                    multiClassGroup = it.multiClassGroup))

            val hscard = it
            Constant.LANGUAGE_LIST.forEach {
                val cardText = cardTranslationMap.getOrPut(it, {mutableMapOf<String, CardTranslation>()})
                var name = hscard.name!![it]

                if (name == null) {
                    System.err.println("no name for $it")
                    name = hscard.name!!["enUS"]
                    if (name == null) {
                        throw Exception()
                    } else {
                        System.err.println("fallback to '$name'")
                    }
                }
                val text = hscard.text?.get(it)
                cardText.put(hscard.id, CardTranslation(name, text))
            }
        }

        System.out.println("${cardDataList.size} cards")

        JsonHelper.encode(Constant.CARD_DATA_JSON, Types.newParameterizedType(List::class.java, Card::class.java), cardDataList)
        for (entry in cardTranslationMap.entries) {
            JsonHelper.encode(Constant.CARD_TRANSLATION_JSON(entry.key), Types.newParameterizedType(Map::class.java, String::class.java, CardTranslation::class.java), entry.value)
        }
    }

    companion object {
        fun hsCardList(): List<HSCard> {
            val hsCardList = JsonHelper.decode<List<HSCard>>(Constant.HSCARDS_JSON, Types.newParameterizedType(List::class.java, HSCard::class.java))

            return hsCardList
                    .filter { it.dbfId != null } // removes "PlaceholderCard"
                    .filter { it.cardClass != null } // removes a bunch of FB_LK_BossSetup cards
        }
    }
}
