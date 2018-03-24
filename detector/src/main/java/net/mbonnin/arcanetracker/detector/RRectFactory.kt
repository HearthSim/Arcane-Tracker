package net.mbonnin.arcanetracker.detector

import timber.log.Timber
import java.nio.ByteOrder

class RRectFactory(val isTablet:Boolean) {

    companion object {

        val RANK_PIXEL = RRect(820.0, 424.0, 230.0, 102.0).scale(1.0/1080, 1.0/1080)
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

    fun rankRect(bbImage: ByteBufferImage): RRect {
        return transformRect(bbImage, if (isTablet) RANK_NEXUS9 else RANK_PIXEL)
    }

    fun transformRect(bbImage: ByteBufferImage, rRect: RRect): RRect {
        if (!isTablet) {
            bbImage.buffer.order(ByteOrder.BIG_ENDIAN)
            val intBuffer = bbImage.buffer.asIntBuffer()

            var left = 0
            while (left < bbImage.w && intBuffer[left].and(0xffffff00.toInt()) == 0){
                left++
            }

            var right = bbImage.w - 1
            while (right >= 0 && intBuffer[right].and(0xffffff00.toInt()) == 0){
                right --
            }
            right++

            Timber.d("left: $left, right: $right, ratio: ${(right - left) / bbImage.h} ")

            if (right - left <= 0) {
                Timber.e("black image ?")
                return RRect(0.0, 0.0, bbImage.w.toDouble(), bbImage.h.toDouble())
            }

            val scale = bbImage.h.toDouble()

            val rect = rRect.scale(scale, scale).translate(left.toDouble(), 0.0)

            Timber.d("rect=%dx%d, %dx%d", rect.x.toInt(), rect.y.toInt(), rect.w.toInt(), rect.h.toInt())
            return rect
        } else {
            return RRect(0.0, 0.0, bbImage.w.toDouble(), bbImage.h.toDouble())
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