buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidPlugin}")
        classpath("com.google.gms:google-services:${Versions.playServicesPlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("com.google.firebase:firebase-crashlytics-gradle:${Versions.crashlyticsPlugin}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
        classpath("com.squareup.sqldelight:gradle-plugin:${Versions.sqldelight}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            // For ktime
            url = uri("https://dl.bintray.com/korlibs/korlibs")
        }
        maven {
            // For kotlinx.serialization
            url = uri("https://kotlin.bintray.com/kotlinx")
        }
        jcenter()
    }
}
