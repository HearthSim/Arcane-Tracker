package net.hearthsim.console

class DefaultConsole : Console {
    override fun debug(message: String) {
        println(message)
    }

    override fun error(message: String) {
        println(message)
    }

    override fun error(throwable: Throwable) {
        println(throwable.message)
        if (throwable.cause != null) {
            println(throwable.message)
        }
    }

}