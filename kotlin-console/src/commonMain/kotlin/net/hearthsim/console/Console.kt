package net.hearthsim.console

interface Console {
    fun debug(message: String)
    fun error(message: String)
    fun error(throwable: Throwable)
}