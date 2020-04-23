package net.mbonnin.jolly

import java.net.URLEncoder

actual object UrlEncoder {
    actual fun encode(src: String): String {
        return URLEncoder.encode(src)
    }
}