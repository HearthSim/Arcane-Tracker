rootProject.name = "arcane-tracker"

// Workaround for: https://youtrack.jetbrains.com/issue/KT-27612
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:${requested.version}")
            }
        }
    }
}


enableFeaturePreview("GRADLE_METADATA")

include ("app", "detector", "kotlin-analytics", "kotlin-console", "kotlin-hslog", "kotlin-deckstring", "kotlin-hsreplay-api", "kotlin-hsmodel")