package net.mbonnin.plugin


import net.mbonnin.hsmodel.hsmodel.CardJson
import java.util.*

class GenerateCardIdsTask {
    fun generate() {
        CardJson.init("enUS", null)
        val list = CardJson.allCards()

        val map = TreeMap<String, ArrayList<String>>()
        for (card in list) {
            try {
                val cardName = card.name
                        .toUpperCase()
                        .replace(" ".toRegex(), "_")
                        .replace("[^A-Z_]".toRegex(), "")
                val allIds = map.computeIfAbsent(cardName) { k -> ArrayList() }
                allIds.add(card.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        val names = ArrayList<String>()
        val ids = ArrayList<String>()
        val keys = TreeSet(map.keys)
        for (key in keys) {
            val allXids = map[key]
            Collections.sort(allXids)

            var i = 0
            for (xid in allXids!!) {
                var name = key
                if (i > 0) {
                    name = name + i
                }
                names.add(name)
                ids.add(xid)
                i++
            }
        }


        val sb = StringBuilder()
        sb.append("package net.mbonnin.hsmodel.cardids;\n")
        sb.append("public final class CardIs {\n")
        for (i in names.indices) {
            sb.append(String.format(Locale.ENGLISH, "public static final String %s = \"%s\";\n", names[i], ids[i]))
        }
        sb.append("}\n")
        print(sb.toString())
    }

}
