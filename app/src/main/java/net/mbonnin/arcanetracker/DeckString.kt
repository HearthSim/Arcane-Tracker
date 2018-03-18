package net.mbonnin.arcanetracker

import android.util.Base64
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*

object DeckString {
    fun parse(pasteData: String): Deck? {
        val lines = pasteData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var deck: Deck? = null

        var name = "imported deck"
        var id = ""
        for (line in lines) {
            if (line.startsWith("### ")) {
                name = line.substring(4)
            } else if (line.startsWith("# Deck ID: ")) {
                id = line.substring("# Deck ID: ".length)
            } else if (!line.startsWith("#")) {
                try {
                    deck = decodeCards(line)
                } catch (e: Exception) {
                    Timber.e(e)
                }

            }
        }

        if (deck == null) {
            return null
        }
        deck.name = name
        deck.id = id

        return deck
    }

    private fun decodeCards(deckstring: String): Deck? {
        val data = Base64.decode(deckstring, Base64.DEFAULT)

        val byteBuffer = ByteBuffer.wrap(data)

        Timber.d("deckstring: %s", deckstring)

        val deck = Deck()
        deck.id = UUID.randomUUID().toString()
        deck.classIndex = -1

        byteBuffer.get() // reserverd
        byteBuffer.get() // version;
        VarInt.getVarInt(byteBuffer) // wild/standard

        val heroCount = VarInt.getVarInt(byteBuffer)
        for (i in 0 until heroCount) {
            val card = CardUtil.getCard(VarInt.getVarInt(byteBuffer))
            if (card != null) {
                deck.classIndex = getClassIndex(card.playerClass)
            }
        }

        deck.cards = HashMap()
        var cardCount = VarInt.getVarInt(byteBuffer)
        for (i in 0 until cardCount) {
            val card = CardUtil.getCard(VarInt.getVarInt(byteBuffer))
            if (card != null) {
                Timber.d("card1: %s", card.name)
                deck.cards[card.id] = 1
            }
        }

        cardCount = VarInt.getVarInt(byteBuffer)
        for (i in 0 until cardCount) {
            val card = CardUtil.getCard(VarInt.getVarInt(byteBuffer))
            if (card != null) {
                Timber.d("card2: %s", card.name)
                deck.cards[card.id] = 2
            }
        }

        cardCount = VarInt.getVarInt(byteBuffer)
        for (i in 0 until cardCount) {
            val card = CardUtil.getCard(VarInt.getVarInt(byteBuffer))
            val c = VarInt.getVarInt(byteBuffer)
            if (card != null) {
                deck.cards[card.id] = c
            }
        }

        if (deck.classIndex < 0) {
            return null
        }

        return if (deck.cards == null) {
            null
        } else deck

    }
}
