import net.arcanetracker.app.ATAppPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("multiplatform")
    kotlin("kapt")
    id("kotlinx-serialization")
    id("kotlin-android-extensions")
    id("io.fabric")
    id("com.github.ben-manes.versions") version ("0.21.0")
}

apply<ATAppPlugin>()

kotlin {
    android()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xallow-result-return-type"
}


android {
    compileSdkVersion(Versions.compileSdkVersion)
    lintOptions {
        disable("MissingTranslation")
    }
    defaultConfig {
        applicationId = "net.mbonnin.arcanetracker"
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
        versionCode = 417
        versionName = "4.17"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    val f = project.file("keystore.properties")

    signingConfigs {
        val props = Properties()
        props.load(f.reader())
        create("mbonnin") {
            keyAlias = props.getProperty("keyAlias")
            if (keyAlias == null) {
                keyAlias = "mbonnin"
            }
            keyPassword = props.getProperty("keyAliasPassword")
            if (keyPassword == null) {
                keyPassword = "password"
            }
            storeFile = project.file(props.getProperty("keyStore"))
            if (storeFile == null) {
                storeFile = project.file("keystore.jks")
            }
            storePassword = props.getProperty("keyAliasPassword")
            if (storePassword == null) {
                storePassword = "password"
            }
        }
    }
    dataBinding {
        isEnabled = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            if (f.exists()) {
                signingConfig = signingConfigs.getByName("mbonnin")
            }
            val f2 = project.file("debug.url")
            val debugUrl = if (f2.exists()) f2.readText() else "\"\""
            buildConfigField("String", "DEBUG_URL", debugUrl)
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            if (f.exists()) {
                signingConfig = signingConfigs.getByName("mbonnin")
            }
            buildConfigField("String", "DEBUG_URL", "\"\"")
        }
    }
    packagingOptions {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/*.kotlin_module")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

androidExtensions {
    isExperimental = true
}


dependencies {

    implementation(Libs.support_v4)
    implementation(Libs.appcompat_v7)
    implementation(Libs.design)
    implementation(Libs.recyclerview_v7)
    implementation(Libs.cardview_v7)

    implementation(Libs.constraintLayout)

    implementation(Libs.paperDb)
    implementation(Libs.okhttp)
    implementation(kotlin("stdlib"))

    implementation(Libs.timber)

    implementation(Libs.rxjava2)
    implementation(Libs.rxAndroid2)
    implementation(Libs.retrofit)
    implementation(Libs.retrofitRx2)
    implementation(Libs.retrofitCoroutines)

    implementation(Libs.play_auth)
    implementation(Libs.play_firebase_core)
    implementation(Libs.play_firebase_messaging)

    implementation(Libs.multidex)
    implementation(Libs.picassoDownloader)
    implementation(Libs.picasso)
    implementation(Libs.ktx)
    implementation(Libs.crashlytics)
    implementation(Libs.room)
    implementation(Libs.rxRoom)
    implementation(Libs.flexbox)
    implementation(Libs.coroutines)
    implementation(Libs.coroutinesAndroid)

    add("kapt", Libs.roomProcessor)

    implementation(project(":detector"))
    implementation(project(":kotlin-hsmodel"))
    implementation(project(":kotlin-hslog"))
    implementation(project(":kotlin-deckstring"))


    testImplementation(Libs.junit)
    testImplementation(Libs.okhttp)

    androidTestImplementation(Libs.test_runner)
    androidTestImplementation(Libs.test_rules)
    androidTestImplementation(Libs.multidex)
    androidTestImplementation(Libs.espressoIntents)
    androidTestImplementation(Libs.espresso)
}
//
//dependencyUpdates.resolutionStrategy = {
//    componentSelection { rules ->
//        rules.all { ComponentSelection selection ->
//            boolean rejected = ["alpha", "beta", "rc", "cr", "m"].any { qualifier ->
//                selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*/
//            }
//            if (rejected) {
//                selection.reject("Release candidate")
//            }
//        }
//    }
//}
//
apply(plugin = "com.google.gms.google-services")
