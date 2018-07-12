package net.mbonnin.arcanetracker

import android.util.Base64
import java.nio.ByteBuffer
import java.util.*

object DeckStringParser {

    fun parse(deckstring: String): Deck? {
        try {
            return parseUnsafe(deckstring)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseUnsafe(deckstring: String): Deck? {
        val data = Base64.decode(deckstring, Base64.DEFAULT)

        val byteBuffer = ByteBuffer.wrap(data)

        val deck = Deck()

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
                //Timber.d("card1: %s", card.name)
                deck.cards[card.id] = 1
            }
        }

        cardCount = VarInt.getVarInt(byteBuffer)
        for (i in 0 until cardCount) {
            val card = CardUtil.getCard(VarInt.getVarInt(byteBuffer))
            if (card != null) {
                //Timber.d("card2: %s", card.name)
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
        if (deck.cards == null) {
            return null
        }

        return deck
    }
}
