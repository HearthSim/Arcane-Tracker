package net.mbonnin.arcanetracker.detector

import java.util.concurrent.atomic.AtomicInteger

class Parallel<T, R: Any>(val input: List<T>, val computeFunction: (T) -> R, val numberOfThreads: Int = 8) {

    val readIndex = AtomicInteger(0)
    val writeIndex = AtomicInteger(0)

    val output = ArrayList<R?>(input.size)

    init {
        for (i in 0 until input.size) {
            output.add(null)
        }
    }

    val lock = Object()

    fun compute(): List<R> {
        for (i in 0 until numberOfThreads) {
            Thread(this::runThread).start()
        }

        while (writeIndex.get() != input.size) {
            synchronized(lock) {
                lock.wait()
            }
        }

        return output.filterNotNull()
    }

    private fun runThread() {

        while (true) {
            val index = readIndex.getAndIncrement()
            if (index >= input.size) {
                break
            }

            val o = computeFunction(input.get(index))

            synchronized(lock) {
                output.set(index, o)
                writeIndex.incrementAndGet()
                lock.notifyAll()
            }
        }
    }

}