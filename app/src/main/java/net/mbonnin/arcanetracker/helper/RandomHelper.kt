package net.mbonnin.arcanetracker.helper

import java.util.*

object RandomHelper {
    fun random(size: Int): String {
        val generator = Random()
        val randomStringBuilder = StringBuilder()
        val c = "abcdefghijklmnopqrstuvwxyz0123456789"

        for (i in 0 until size) {
            randomStringBuilder.append(c[generator.nextInt(c.length)])
        }
        return randomStringBuilder.toString()
    }
}