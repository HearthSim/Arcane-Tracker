package net.hearthsim.hsreplay.model

import kotlinx.serialization.Serializable

@Serializable
class Token(val access_token: String, val refresh_token: String)