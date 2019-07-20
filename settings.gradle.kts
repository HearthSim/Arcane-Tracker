rootProject.name = "arcane-tracker"

pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}

include ("app", "detector", "kotlin-analytics", "kotlin-console", "kotlin-hslog", "kotlin-deckstring", "kotlin-hsreplay-api", "kotlin-hsmodel")