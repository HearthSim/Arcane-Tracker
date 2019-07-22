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
    jvm()
    android {
        publishAllLibraryVariants()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(Libs.corountinesCommon)
                implementation(Libs.serializationRuntimeCommon)

                api(Libs.ktorClientCore)
                api(project(":kotlin-console"))
                api(project(":kotlin-analytics"))

                implementation(Libs.ktorClientJson)
                implementation(Libs.ktorClientSerialization)
                implementation(Libs.ktorClientEncoding)
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(Libs.coroutines)
                implementation(Libs.serializationRuntime)
                implementation(Libs.ktorClientSerializationJvm)

                api(Libs.ktorClientCoreJvm)
                implementation(Libs.ktorClientJsonJvm)
                implementation(Libs.ktorClientOkhttp)
                implementation(Libs.ktorClientEncodingJvm)
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(Libs.okhttpLogging)
                implementation(Libs.coroutines)
                implementation(Libs.kotlinTestJvm)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(Libs.coroutines)
                implementation(Libs.serializationRuntime)
                implementation(Libs.ktorClientSerializationJvm)

                api(Libs.ktorClientCoreJvm)
                implementation(Libs.ktorClientJsonJvm)
                implementation(Libs.ktorClientOkhttp)
                implementation(Libs.ktorClientEncodingJvm)
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xallow-result-return-type"
}

android {
    compileSdkVersion(Versions.compileSdkVersion)
    defaultConfig {
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
    }
}
