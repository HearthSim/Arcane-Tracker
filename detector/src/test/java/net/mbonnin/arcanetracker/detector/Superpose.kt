package net.mbonnin.arcanetracker.detector

import com.google.gson.JsonParser
import net.mbonnin.arcanetracker.detector.Detector.Companion.NAME_TO_CARD_ID
import net.mbonnin.arcanetracker.detector.RRectFactory.Companion.ARENA_MINIONS_PIXEL
import net.mbonnin.arcanetracker.detector.RRectFactory.Companion.ARENA_SPELLS_PIXEL
import net.mbonnin.arcanetracker.detector.RRectFactory.Companion.ARENA_WEAPONS_PIXEL
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.Type
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader


class Superpose {
    val DATA_ROOT = "/home/martin/git/arcane_data"
    val OUTPUT_DIR = "/home/martin/tmp/superpose"



    @Test
    fun superpose() {
        val outFile = File(OUTPUT_DIR)
        if (!outFile.exists()) {
            outFile.mkdirs()
        }

        CardJson.init("enUS")

        val reader = InputStreamReader(CardJson::class.java.getResourceAsStream("/arena_choices.json"))
        val choices = JsonParser().parse(reader).asJsonObject

        Parallel(choices.entrySet().toList(), {
            val fileIndex = it.key

            val inputFile = File(DATA_ROOT, "/tests/arena_choices/" + fileIndex + ".png")
            val choiceImage = pngToByteBufferImage(FileInputStream(inputFile))

            it.value.asJsonArray.forEachIndexed { index, name ->
                val cardId = NAME_TO_CARD_ID[name.asString]
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
            Type.MINION -> ARENA_MINIONS_PIXEL[index]
            Type.SPELL -> ARENA_SPELLS_PIXEL[index]
            Type.WEAPON -> ARENA_WEAPONS_PIXEL[index]
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