package net.arcanetracker.app

import net.arcanetracker.ProcessHelper
import net.arcanetracker.ProcessHelper.execOrFail
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ExtractUnityFilesTask: DefaultTask() {
    @TaskAction
    fun taskAction() {
        val dir = project.buildDir.absolutePath + "/boomsday_pre/"
        val result = ProcessHelper.exec("adb shell su -c ls /data/data/com.blizzard.wtcg.hearthstone/files/Data/dxt/*.unity3d")

        if (result.errCode != 0) {
            System.err.println("cannot run adb shell. Make sure adb is in your path")
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
}
