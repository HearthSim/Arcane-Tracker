package net.mbonnin.arcanetracker.detector

class RRect(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun scale(sx: Double, sy: Double): RRect = RRect(sx * x, sy * y, sx * w, sy * h)
    fun translate(tx: Double, ty: Double): RRect = RRect(tx + x, ty + y, w, h)
}

