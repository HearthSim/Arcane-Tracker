import Versions.compileSdkVersion
import Versions.kotlin
import Versions.minSdkVersion
import Versions.targetSdkVersion

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    jvm()
    android {
        publishAllLibraryVariants()
    }
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
                api(Libs.kotlinxIo)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.stdlibJdk8)
                api(Libs.kotlinxIoJvm)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))
                api(Libs.kotlinxIoJvm)
            }
        }
        macosX64().compilations["main"].defaultSourceSet {
            dependencies {
                api(Libs.kotlinxIoNative)
            }
        }
    }
}

android {
    compileSdkVersion(Versions.compileSdkVersion)
    defaultConfig {
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
    }
}
