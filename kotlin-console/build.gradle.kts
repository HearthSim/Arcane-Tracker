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
            }
        }
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(Libs.stdlibJdk8)
            }
        }
    }
}
