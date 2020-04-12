#!/usr/bin/env kscript
//@file:MavenRepository("mavenLocal", "file://Users/m.bonnin/.m2/repository/")
@file:MavenRepository("jcenter", "https://jcenter.bintray.com")
@file:MavenRepository("gradle", "https://repo.gradle.org/gradle/libs-releases-local/")
@file:DependsOn("com.dailymotion.kinta:kinta-lib:0.1.6")

import com.dailymotion.kinta.KintaEnv
import com.dailymotion.kinta.integration.android.AndroidIntegration
import com.dailymotion.kinta.integration.github.GithubActions
import com.dailymotion.kinta.integration.github.GithubIntegration
import com.dailymotion.kinta.integration.zip.ZipIntegration
import java.io.File

fun zip(dir: String, zipName: String): File {
    val dirFile = File(dir)
    val output = File(dirFile.parentFile, zipName)
    ZipIntegration.zip(input = dirFile, baseDir = dirFile.parentFile, output = output)

    return output
}

println("Signing APK...")

AndroidIntegration.signApk(
    input = File("app/build/outputs/apk/release/app-release-unsigned.apk"),
    output = File("app/build/outputs/apk/release/app-release.apk")
)

if (GithubActions.isTag()) {
    println("creating release on github...")

    val assets = listOf(
        zip("kotlin-hslog/build/bin/macosX64/debugFramework/kotlin_hslog.framework", "kotlin_hslog.framework.debug.zip"),
        zip("kotlin-hslog/build/bin/macosX64/debugFramework/kotlin_hslog.framework.dSYM", "kotlin_hslog.framework.dSYM.debug.zip"),
        zip("kotlin-hslog/build/bin/macosX64/releaseFramework/kotlin_hslog.framework", "kotlin_hslog.framework.release.zip"),
        File("app/build/outputs/apk/debug/app-debug.apk"),
        File("app/build/outputs/apk/release/app-release.apk")
    )

    GithubIntegration.createRelease(
        token = KintaEnv.get("KINTA_GITHUB_TOKEN"),
        tagName = GithubActions.tagName(),
        assets = assets,
        repo = "Arcane-Tracker",
        owner = "Hearthsim",
        changelogMarkdown = ""
    )
}