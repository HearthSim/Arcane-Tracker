package net.hearthsim.hsreplay

sealed class HSReplayResult<T> {
    class Success<T>(val value: T): HSReplayResult<T>()
    class Error<T>(val exception: Exception): HSReplayResult<T>()
}