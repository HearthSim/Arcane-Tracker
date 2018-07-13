package net.arcanetracker

object ProcessHelper {
    data class Result(val errCode: Int, val out: String)

    fun exec(cmd: String): Result {
        val process = ProcessBuilder().command(cmd.split(" "))
                .start()

        val inputStream = process.inputStream

        val errCode = process.waitFor()

        return Result(errCode, inputStream.bufferedReader().use { it.readText() })
    }


    fun execOrFail(cmd: String): String {
        val result = exec(cmd)

        if (result.errCode != 0) {
            throw Exception("cannot execute $cmd: ${result.out}")
        }

        return result.out
    }
}