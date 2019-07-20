import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
                implementation(Libs.stdlibJdk8)

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
                implementation(Libs.kotlinTestAnnotationJvm)
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xallow-result-return-type"
}
