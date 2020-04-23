
plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    jvm()
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
                api(Libs.okio)
                implementation(Libs.serializationRuntime)
            }
        }
    }
}
