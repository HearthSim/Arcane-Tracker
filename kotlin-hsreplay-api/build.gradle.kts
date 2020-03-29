import Versions.compileSdkVersion
import Versions.minSdkVersion
import Versions.targetSdkVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    jvm() {
        val main by compilations.getting {
            kotlinOptions {
                // Setup the Kotlin compiler options for the 'main' compilation:
                jvmTarget = "1.8"
            }
        }
    }
    android {
        publishAllLibraryVariants()
    }
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(Libs.coroutines)
                implementation(Libs.serializationRuntimeCommon)

                api(Libs.ktorClientCore)
                api(project(":kotlin-console"))
                api(project(":kotlin-analytics"))

                implementation(Libs.ktorClientJson)
                implementation(Libs.ktorClientSerialization)
                implementation(Libs.ktorClientEncoding)
                implementation(Libs.ktorClientLogging)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(Libs.serializationRuntime)
                implementation(Libs.ktorClientOkhttp)
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(Libs.okhttpLogging)
                implementation(Libs.kotlinTestJvm)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(Libs.serializationRuntime)
                implementation(Libs.ktorClientOkhttp)
            }
        }
        macosX64().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.serializationRuntimeMacOS)

                implementation(Libs.ktorClientCurl)
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
