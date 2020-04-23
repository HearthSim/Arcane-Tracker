package net.mbonnin.jolly

expect object UrlEncoder {
    fun encode(src: String): String
}