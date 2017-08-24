package net.mbonnin.arcantracker.detector

import java.nio.ByteBuffer

data class APlane(val buffer: ByteBuffer, val stride: Int)
data class AImage(val w: Int, val h: Int, val planes: Array<APlane>)

class Detector {
    fun detectRank(aImage: AImage) {


    }
}


