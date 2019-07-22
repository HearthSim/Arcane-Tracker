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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.stdlibJdk8)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))
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
