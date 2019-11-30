buildscript {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.fabric.io/public")
        }
//        maven {
//            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
//        }
        jcenter() // for trove4j
        maven {
            url = uri("https://dl.bintray.com/nimroddayan/buildmetrics")
        }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidPlugin}")
        classpath("com.google.gms:google-services:${Versions.playServicesPlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("io.fabric.tools:gradle:${Versions.fabricPlugin}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}")
    }
}


plugins {
    id("com.nimroddayan.buildmetrics.googleanalytics").version("0.1.0")
}

googleAnalytics {
    trackingId.set("UA-122919265-2")
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
//        maven {
//            url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
//        }
        jcenter()
    }
}
