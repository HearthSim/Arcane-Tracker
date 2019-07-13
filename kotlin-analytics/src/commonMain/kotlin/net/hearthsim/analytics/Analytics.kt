package net.hearthsim.analytics

interface Analytics {
    fun logEvent(name: String, params: Map<String, String?> = emptyMap())
}