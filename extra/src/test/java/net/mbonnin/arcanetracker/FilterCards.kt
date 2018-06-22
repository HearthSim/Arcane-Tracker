package net.mbonnin.arcanetracker

import com.squareup.moshi.Types
import net.mbonnin.hsmodel.HSSet.LOOTAPALOOZA
import org.junit.Test

class FilterCards {
    @Test
    fun run() {
        val hsCardList = JsonHelper.decode<List<HSCard>>(Constant.HSCARDS_JSON, Types.newParameterizedType(List::class.java, HSCard::class.java))

        for (hsCard in hsCardList) {
            if (hsCard.type == "SPELL"
                && hsCard.mechanics?.contains("SECRET") == true
                    && hsCard.set == LOOTAPALOOZA) {
                System.err.println("secret: ${hsCard.id}")
            }
        }
    }
}