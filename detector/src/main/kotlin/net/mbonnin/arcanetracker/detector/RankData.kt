package net.mbonnin.arcanetracker.detector

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

data class RankData(val RANKS: List<DoubleArray>)

/**
 * serialization doesn't know how to serialize DoubleArray so we use this proxy class...
 */
@Serializable
class SerializableRankData(val RANKS: List<List<Double>>)


fun SerializableRankData.toRankData(): RankData {
    return RankData(RANKS.map { list ->
        DoubleArray(list.size) { list.get(it) }
    })
}

fun RankData.serializable(): SerializableRankData {
    return SerializableRankData(RANKS.map { it.toList() })
}
