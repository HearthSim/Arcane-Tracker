package net.arcanetracker.hsmodel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HSCard(val id: String,
                  val text: Map<String, String>?,
                  val name: Map<String, String>?,
                  val cardClass: String?,
                  val rarity: String?,
                  val type: String?,
                  val race: String?,
                  val set: String?,
                  val dbfId: Int?,
                  val cost: Int?,
                  val attack: Int?,
                  val health: Int?,
                  val durability: Int?,
                  val collectible: Boolean?,
                  val multiClassGroup: String?,
                  val mechanics: List<String>?
)