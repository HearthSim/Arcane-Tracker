package net.mbonnin.jolly

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters

actual object UrlEncoder {
    actual fun encode(src: String): String {
        return (src as NSString).stringByAddingPercentEncodingWithAllowedCharacters(
                allowedCharacters = NSCharacterSet.URLQueryAllowedCharacterSet
        )!!
    }
}