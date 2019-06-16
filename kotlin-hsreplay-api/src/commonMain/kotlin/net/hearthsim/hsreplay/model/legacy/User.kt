package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
class User(val battleTag: String,
           val id: String,
           val username: String)
