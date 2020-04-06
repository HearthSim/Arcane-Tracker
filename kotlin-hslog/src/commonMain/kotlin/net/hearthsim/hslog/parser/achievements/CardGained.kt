package net.hearthsim.hslog.parser.achievements

class CardGained(val id: String, val golden: Boolean) {
    override fun toString(): String {
        if (golden) {
            return "$id*"
        } else {
            return id
        }
    }
}