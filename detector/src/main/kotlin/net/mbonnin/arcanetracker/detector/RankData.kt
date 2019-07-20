package net.mbonnin.arcanetracker.detector

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RankData(val RANKS: List<DoubleArray>)