package net.mbonnin.arcanetracker

import org.junit.Test
import java.io.File

class ExtractUnityFiles {
    @Test
    fun run() {
        val dir = "/home/martin/dev/hearthsim/tavern_of_time_unity/"
        val result = exec("adb shell su -c ls /data/data/com.blizzard.wtcg.hearthstone/files/Data/dxt/*.unity3d")

        if (result.errCode != 0) {
            System.err.println("cannot run adb shell")
            return
        }

        val files = result.out.split("\n")
                .filter { !it.isBlank() }
                .map { it.trim() }

        var i = 0
        for (file in files) {
            System.out.print("${i++}/${files.size} ")

            val name = file.substringAfterLast("/")
            val outFile = File(dir, name)

            if (outFile.exists()) {
                val remoteLs =  execOrFail("adb shell su -c ls -l $file")

                val size = remoteLs.split(" ")[4].toLong()

                val localSize = outFile.length()

                if (size == localSize) {
                    System.out.println("skip $file")
                    continue
                }
            }

            System.out.println("copy $file")
            execOrFail("adb shell su -c cp $file /sdcard/$name")
            execOrFail("adb pull /sdcard/$name ${outFile.canonicalPath}")
            execOrFail("adb shell rm /sdcard/$name")
        }
    }

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