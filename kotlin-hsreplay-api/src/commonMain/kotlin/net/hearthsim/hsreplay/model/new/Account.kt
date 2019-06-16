package net.hearthsim.hsreplay.model.new

import kotlinx.serialization.Serializable

@Serializable
class Account(
        val battletag: String,
        val id: String,
        val is_premium: Boolean?,
        val username: String
)