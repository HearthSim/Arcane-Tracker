package net.mbonnin.arcanetracker.hslog.power

enum class FormatType(val intValue: Int) {
    FT_UNKNOWN(0),
    FT_WILD(1),
    FT_STANDARD(2),
}


fun fromFormatTypeString(format: String?): FormatType {
    FormatType.values().forEach {
        if (it.name == format) {
            return it
        }
    }

    return FormatType.FT_UNKNOWN
}