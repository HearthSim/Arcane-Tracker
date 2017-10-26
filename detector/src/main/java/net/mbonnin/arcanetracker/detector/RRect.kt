package net.mbonnin.arcanetracker.detector

open class RRect(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun scale(sx: Double, sy: Double): RRect = RRect(sx * x, sy * y, sx * w, sy * h)
}

