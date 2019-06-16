package net.hearthsim.hsreplay.model.legacy

import kotlinx.serialization.Serializable

@Serializable
data class UploadToken(val key: String, val user: User)
