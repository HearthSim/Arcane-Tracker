import Versions.compileSdkVersion
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
                api(Libs.okio)
                implementation(Libs.serializationRuntimeCommon)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.stdlibJdk8)
                implementation(Libs.serializationRuntime)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(Libs.serializationRuntime)
            }
        }
        macosX64().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.serializationRuntimeMacOS)
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
