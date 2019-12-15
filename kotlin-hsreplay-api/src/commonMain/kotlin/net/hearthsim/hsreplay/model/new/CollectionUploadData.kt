package net.hearthsim.hsreplay.model.new

import kotlinx.serialization.Serializable

@Serializable
class CollectionCard(
        val cardId: String,
        val count: Int,
        val premium: Boolean
)

@Serializable
class CollectionUploadData(
        val collection: List<CollectionCard>,
        val favoriteHeroes: Map<Int, CollectionCard>,
        val cardbacks: List<Int>,
        val favoriteCardback: Int?,
        val dust: Int?,
        val gold: Int?
)