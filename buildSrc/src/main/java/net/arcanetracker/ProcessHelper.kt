package net.arcanetracker

object ProcessHelper {
    data class Result(val errCode: Int, val out: String, val err: String)

    fun exec(cmd: String): Result {
        val process = ProcessBuilder().command(cmd.split(" "))
                .start()


        val errCode = process.waitFor()

        val outStream = process.inputStream
        val errStream = process.errorStream
        return Result(errCode, outStream.bufferedReader().use { it.readText() }, errStream.bufferedReader().use { it.readText() })
    }


    fun execOrFail(cmd: String): String {
        val result = exec(cmd)

        if (result.errCode != 0) {
            throw Exception("cannot execute $cmd: ${result.out}")
        }

        return result.out
    }
}