package net.mbonnin.arcanetracker.detector

import com.google.gson.JsonParser
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.Type
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*


class Superpose {
    val DATA_ROOT = "/home/martin/git/arcane_data"
    val OUTPUT_DIR = "/home/martin/tmp/superpose"

    val RECTS_MINION = arrayOf(
            RRect(324.0, 258.0, 208.0, 208.0),
            RRect(844.0, 258.0, 208.0, 208.0),
            RRect(1364.0, 258.0, 208.0, 208.0)
    )

    val RECTS_SPELLS = arrayOf(
            RRect(331.0, 280.0, 189.0, 189.0),
            RRect(850.0, 280.0, 189.0, 189.0),
            RRect(1369.0, 280.0, 189.0, 189.0)
    )

    val WEAPON_SPELLS = arrayOf(
            RRect(347.0, 270.0, 173.0, 173.0),
            RRect(870.0, 270.0, 173.0, 173.0),
            RRect(1391.0, 270.0, 173.0, 173.0)
    )

    @Test
    fun superpose() {
        val reader = InputStreamReader(CardJson::class.java.getResourceAsStream("/arena_choices.json"))
        val outFile = File(OUTPUT_DIR)
        if (!outFile.exists()) {
            outFile.mkdirs()
        }

        CardJson.init("enUS")

        val map = TreeMap<String, ArrayList<String>>()

        CardJson.allCards().filter { it.name != null }.forEach({
            val cardName = it.name!!
                    .toUpperCase()
                    .replace(" ", "_")
                    .replace(Regex("[^A-Z_]"), "")

            map.getOrPut(cardName, { ArrayList() }).add(it.id)
        })

        val nameToCardID = TreeMap<String, String>()

        for (entry in map) {
            entry.value.sort()
            for ((i, id) in entry.value.withIndex()) {
                var name = entry.key
                if (i > 0) {
                    name += i
                }
                nameToCardID.put(name, id)
            }
        }

        val choices = JsonParser().parse(reader).asJsonObject

        Parallel(choices.entrySet().toList(), {
            val fileIndex = it.key

            val inputFile = File(DATA_ROOT, "/tests/arena_choices/" + fileIndex + ".png")
            val choiceImage = pngToByteBufferImage(FileInputStream(inputFile))

            it.value.asJsonArray.forEachIndexed { index, name ->
                val cardId = nameToCardID[name.asString]
                if (cardId != null) {
                    superposeModel(choiceImage, index, cardId)
                } else {
                    Assert.fail("No mapping for name " + name.asString)
                }
            }
            byteBufferImageToPng(choiceImage, File(OUTPUT_DIR, fileIndex + ".png"))
        }).compute()
    }

    fun convertStreamToString(stream: java.io.InputStream): String {
        val s = java.util.Scanner(stream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

    private fun superposeModel(choiceImage: ByteBufferImage, index: Int, cardId: String) {
        val rect = when(CardJson.getCard(cardId)?.type) {
            Type.MINION -> RECTS_MINION[index]
            Type.SPELL -> RECTS_SPELLS[index]
            Type.WEAPON -> WEAPON_SPELLS[index]
            else -> null
        }

        if (rect == null) {
            System.err.println("no rect for" + CardJson.getCard(cardId)?.name)
            return
        }

        val pngCard = String.format("%s/card%s.png", OUTPUT_DIR, cardId)
        val process = Runtime.getRuntime().exec(String.format("convert %s/models/cards/%s.jpg  -crop 250x250+120+120 -resize %sx%s %s", DATA_ROOT, cardId, rect.w.toInt(), rect.h.toInt(), pngCard))
        if (process.waitFor() != 0) {
            Assert.fail("cannot execute resize;" + convertStreamToString(process.errorStream))
        }
        val modelFile = File(pngCard)
        val cardImage = pngToByteBufferImage(FileInputStream(modelFile))

        val choiceBuffer = choiceImage.buffer
        val cardBuffer = cardImage.buffer

        for (x in 0 until rect.w.toInt()) {
            for (y in 0 until rect.h.toInt()) {
                val offset = ((x + rect.x) * 4 + (y + rect.y) * choiceImage.stride).toInt()
                val cardOffset = (x * 4 + y * cardImage.stride)
                for (i in 0 until 3) {
                    val c1 = choiceBuffer.get(offset + i)
                    val c2 = if (i == 3) 255.toByte() else cardBuffer.get(cardOffset + i)

                    choiceBuffer.put(offset + i, c2)
                }
            }
        }
    }
}