plugins {
    kotlin("multiplatform")
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
    macosX64 {
        binaries {
            framework {
                export(project(":kotlin-hsmodel"))
                export(project(":kotlin-hsreplay-api"))
            }
        }
    }

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
        val jvmTest by getting {
            dependencies {
                implementation(Libs.kotlinTestJvm)
                implementation(Libs.serializationRuntime)
            }
        }
        val macosX64Main by getting {
            dependencies {
                // Not really 100% sure why this is not inherited from the commonMain dependencies but it's required
                // to have auttocompletion
                api(project(":kotlin-hsmodel"))
                api(project(":kotlin-hsreplay-api"))
                api(project(":kotlin-console"))
            }
        }
    }
}
