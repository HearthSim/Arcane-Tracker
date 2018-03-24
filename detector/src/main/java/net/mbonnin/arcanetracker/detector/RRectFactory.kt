package net.mbonnin.arcanetracker.detector

import timber.log.Timber
import java.nio.ByteOrder

class RRectFactory(val isTablet: Boolean) {

    companion object {

        val PLAYER_RANK_PIXEL2 = RRect(2461.0, 1230.0, 202.0, 89.0).scale(1.0 / 1440, 1.0 / 1440)
        val RANK_OPPONENT_PIXEL_2XL = RRect(2461.0, 1230.0, 202.0, 89.0).scale(1.0 / 1440, 1.0 / 1440)

        val RANK_NEXUS9 = RRect(1730.0, 201.0, 110.0, 46.0)

        val ARENA_MINIONS_PIXEL = arrayOf(
                RRect(324.0, 258.0, 208.0, 208.0),
                RRect(844.0, 258.0, 208.0, 208.0),
                RRect(1364.0, 258.0, 208.0, 208.0)
        )

        val ARENA_SPELLS_PIXEL = arrayOf(
                RRect(331.0, 280.0, 189.0, 189.0),
                RRect(850.0, 280.0, 189.0, 189.0),
                RRect(1369.0, 280.0, 189.0, 189.0)
        )

        val ARENA_WEAPONS_PIXEL = arrayOf(
                RRect(347.0, 270.0, 173.0, 173.0),
                RRect(870.0, 270.0, 173.0, 173.0),
                RRect(1391.0, 270.0, 173.0, 173.0)
        )
    }

    fun playerRankRect(bbImage: ByteBufferImage): RRect {
        val intBuffer = bbImage.buffer.asIntBuffer()

        val mask = if (bbImage.buffer.order() == ByteOrder.BIG_ENDIAN) 0xffffff00 else 0x00ffffff

        var left = 0
        while (left < bbImage.w && intBuffer[left].and(mask.toInt()) == 0) {
            left++
        }

        var right = bbImage.w - 1
        while (right >= 0 && intBuffer[right].and(mask.toInt()) == 0) {
            right--
        }
        right++

        Timber.d("left: $left, right: $right, ratio: ${(right - left) / bbImage.h} ")

        if (right - left <= 0) {
            Timber.e("black image ?")
            return RRect(0.0, 0.0, bbImage.w.toDouble(), bbImage.h.toDouble())
        }

        val y = 1231.0 * bbImage.h / 1440.0
        val h = 87.0 * bbImage.h / 1440.0
        val x = right - 216 * bbImage.h / 1440.0
        val w = 198 * bbImage.h / 1440.0

        return RRect(x, y, w, h)
    }

    fun opponentRankRect(bbImage: ByteBufferImage): RRect {
        val intBuffer = bbImage.buffer.asIntBuffer()

        val mask = if (bbImage.buffer.order() == ByteOrder.BIG_ENDIAN) 0xffffff00 else 0x00ffffff

        var left = 0
        while (left < bbImage.w && intBuffer[left].and(mask.toInt()) == 0) {
            left++
        }

        var right = bbImage.w - 1
        while (right >= 0 && intBuffer[right].and(mask.toInt()) == 0) {
            right--
        }
        right++

        Timber.d("left: $left, right: $right, ratio: ${(right - left) / bbImage.h} ")

        if (right - left <= 0) {
            Timber.e("black image ?")
            return RRect(0.0, 0.0, bbImage.w.toDouble(), bbImage.h.toDouble())
        }

        val y = 1231.0 * bbImage.h / 1440.0
        val h = 87.0 * bbImage.h / 1440.0
        val x = left + 35 * bbImage.h / 1440.0
        val w = 198 * bbImage.h / 1440.0

        return RRect(x, y, w, h)
    }

    fun arenaMinionRectArray(): Array<RRect> {
        return ARENA_MINIONS_PIXEL
    }

    fun arenaSpellRectArray(): Array<RRect> {
        return ARENA_SPELLS_PIXEL
    }

    fun arenaWeaponRectArray(): Array<RRect> {
        return ARENA_WEAPONS_PIXEL
    }
}