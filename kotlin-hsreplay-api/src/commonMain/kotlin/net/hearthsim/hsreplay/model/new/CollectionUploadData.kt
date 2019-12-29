package net.hearthsim.hsreplay.model.new

import kotlinx.serialization.Serializable


@Serializable
class CollectionUploadData(
        /*
         * key is the dfbId as a string
         * value is a list of 2 ints: one for the number of non-gold and one for the number of gold cards
         */
        val collection: Map<String, List<Int>>,
        /*
         * key is the playerClass int
         * value is the dfbId of the given hero as a int
         */
        val favoriteHeroes: Map<String, Int>,
        val cardbacks: List<Int>,
        val favoriteCardback: Int?,
        val dust: Int?,
        val gold: Int?
)