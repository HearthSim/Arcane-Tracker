import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
                implementation(Libs.klock)
                implementation(project(":kotlin-deckstring"))
                api(project(":kotlin-hsmodel"))
                api(project(":kotlin-hsreplay-api"))
                api(project(":kotlin-console"))
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(Libs.kotlinTestJvm)
                implementation(Libs.kotlinTestAnnotationJvm)
                implementation(Libs.serializationRuntime)
            }
        }
    }
}
