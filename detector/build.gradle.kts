plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    android()
}

android {
    compileSdkVersion(Versions.compileSdkVersion)
    splits {
        abi {
            reset()
            include("arm64-v8a", "armeabi-v7a")
        }
    }
    defaultConfig {
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
        versionCode = 1
        versionName = "1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++11")
            }
        }
    }

    externalNativeBuild {
        cmake {
            setPath("CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(Libs.timber)
    implementation(Libs.serializationRuntime)

    testImplementation(Libs.junit)
    testImplementation(Libs.pngj)

    androidTestImplementation(Libs.test_runner)
    androidTestImplementation(Libs.test_rules)
    androidTestImplementation(Libs.espresso)
    androidTestImplementation(Libs.okhttp)
}

