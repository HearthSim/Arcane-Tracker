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
                implementation(Libs.coroutines)
                implementation(Libs.serializationRuntime)
                api(Libs.okio)

                api(project(":kotlin-console"))
                api(project(":kotlin-analytics"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(Libs.kotlinTestCommon)
                implementation(Libs.kotlinTestAnnotationCommon)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(Libs.okhttp)
                implementation(Libs.okhttpLogging)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(Libs.kotlinTestJvm)
            }
        }

        val macosX64Main by getting {
            dependencies {
                api(Libs.okio)
            }
        }
    }
}
