package net.mbonnin.arcanetracker.hsreplay.model

data class Lce<T> private constructor(val data: T?, val error: Throwable?) {
    val isLoading = data == null && error == null

    companion object {
        fun <T> data(data: T): Lce<T> {
            return Lce(data, null)
        }

        fun <T> error(error: Throwable): Lce<T> {
            return Lce<T>(null, error)
        }

        fun <T> loading(): Lce<T> {
            return Lce<T>(null, null)
        }
    }
}