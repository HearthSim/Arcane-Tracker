import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
                api(Libs.kotlinxIo)
                implementation(Libs.serializationRuntimeCommon)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.stdlibJdk8)
                api(Libs.kotlinxIoJvm)
                implementation(Libs.serializationRuntime)
            }
        }
    }
}
