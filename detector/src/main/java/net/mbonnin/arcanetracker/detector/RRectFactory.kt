package net.mbonnin.arcanetracker.detector

import timber.log.Timber
import java.nio.ByteOrder

class RRectFactory(val isTablet: Boolean) {

    var left = 0
    var right = 0

    companion object {
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

    fun prepareImage(bbImage: ByteBufferImage) {
        val intBuffer = bbImage.buffer.asIntBuffer()

        val mask = if (bbImage.buffer.order() == ByteOrder.BIG_ENDIAN) 0xffffff00 else 0x00ffffff

        left = 0
        while (left < bbImage.w && intBuffer[left].and(mask.toInt()) == 0) {
            left++
        }

        right = bbImage.w - 1
        while (right >= 0 && intBuffer[right].and(mask.toInt()) == 0) {
            right--
        }
        right++

        Timber.d("left: $left, right: $right, ratio: ${(right - left) / bbImage.h} ")

        if (right - left <= 0) {
            left = 0
            right = bbImage.w
            Timber.e("black image ?")
        }
    }

    fun playerRankRect(bbImage: ByteBufferImage): RRect {
        if (!isTablet) {
            // The hearthstone window doesn't always have the same aspect ratio (1.85 on pixel 2 XL, 1.77 on galaxy S8)
            // so we do everything relative to the height
            return RRect(right - 216 * bbImage.h / 1440.0,
                    1231.0 * bbImage.h / 1440.0,
                    198 * bbImage.h / 1440.0,
                    87.0 * bbImage.h / 1440.0)
        } else {
            return RRect(left + 15 * bbImage.h / 1200.0,
                    56.0 * bbImage.h / 1200.0,
                    87 * bbImage.h / 1200.0,
                    37.0 * bbImage.h / 1200.0)
        }
    }

    fun opponentRankRect(bbImage: ByteBufferImage): RRect {
        if (!isTablet) {
            return RRect(left + 35 * bbImage.h / 1440.0,
                    1231.0 * bbImage.h / 1440.0,
                    198 * bbImage.h / 1440.0,
                    87.0 * bbImage.h / 1440.0)
        } else {
            return RRect(left + 15 * bbImage.h / 1200.0,
                    1015.0 * bbImage.h / 1200.0,
                    87 * bbImage.h / 1200.0,
                    37.0 * bbImage.h / 1200.0)

        }
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