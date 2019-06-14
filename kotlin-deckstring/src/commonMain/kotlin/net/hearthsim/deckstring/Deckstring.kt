package net.hearthsim.deckstring

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import kotlinx.io.core.toByteArray


object Deckstring {
    var FT_STANDARD = 1
    var FT_WILD = 2

    private val VERSION = 1

    class Result {
        var heroes: MutableList<Int> = mutableListOf()
        var format: Int = FT_STANDARD
        var cards: MutableList<Card> = mutableListOf()
    }

    private fun getVarInt(src: Input): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            if (shift >= 32) {
                // Out of range
                throw IndexOutOfBoundsException("varint too long")
            }
            // Get 7 bits from next byte
            b = src.readByte().toInt()
            result = result or (b and 0x7F shl shift)
            shift += 7
        } while (b and 0x80 != 0)
        return result
    }

    class Card(var dbfId: Int, var count: Int // number of times this card appears in the deck
    )

    class ParseException internal constructor(message: String) : Exception(message)


    /**
     *
     * @param deckString the base64 encoded deckstring as you would copy/past in the game
     * @return
     * @throws Exception
     */
    fun decode(deckString: String): Result {
        return decode(ByteReadPacket(deckString.toByteArray()))
    }

    /**
     *
     * @param data the base64 decoded data.
     * @return
     * @throws Exception
     */
    fun decode(data: Input): Result {
        val result = Result()
        
        data.readByte()
        val version = data.readByte().toInt()
        if (version != VERSION) {
            throw ParseException("bad version: $version")
        }

        result.format = getVarInt(data)
        if (result.format != FT_STANDARD && result.format != FT_WILD) {
            throw ParseException("bad format: " + result.format)
        }


        val heroCount = getVarInt(data)
        result.heroes = ArrayList()
        for (i in 0 until heroCount) {
            result.heroes.add(getVarInt(data))
        }

        result.cards = mutableListOf()
        for (i in 1..3) {
            val c = getVarInt(data)
            for (j in 0 until c) {
                val dbfId = getVarInt(data)
                val count: Int
                if (i == 3) {
                    count = getVarInt(data)
                } else {
                    count = i
                }
                result.cards.add(Card(dbfId, count))
            }
        }

        result.cards.sortedBy {
            it.dbfId
        }

        return result
    }
}
