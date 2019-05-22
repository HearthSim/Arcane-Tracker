package net.mbonnin.hsmodel

import kotlinx.serialization.Serializable

@Serializable
data class HSCard(val id: String,
                  val text: Map<String, String>? = null,
                  val name: Map<String, String>? = null,
                  val cardClass: String? = null,
                  val rarity: String? = null,
                  val type: String? = null,
                  val race: String? = null,
                  val set: String? = null,
                  val dbfId: Int? = null,
                  val cost: Int? = null,
                  val attack: Int? = null,
                  val health: Int? = null,
                  val durability: Int? = null,
                  val collectible: Boolean? = null,
                  val multiClassGroup: String? = null,
                  val mechanics: List<String>? = null
)