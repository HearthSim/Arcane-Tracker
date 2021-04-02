object Versions {
    const val minSdkVersion = 21
    const val targetSdkVersion = 29
    const val compileSdkVersion = 29
    const val kotlin = "1.3.72"
    const val androidPlugin = "3.6.2"
    const val playServicesPlugin = "4.3.3"
    const val crashlyticsPlugin = "2.1.1"
    const val coroutines = "1.3.5"
    const val ktor = "1.3.2"
    const val serialization = "0.20.0"
    const val sqldelight = "1.3.0"
}

object Libs {
    const val supportLibVersion = "28.0.0"

    const val support_v4 = "com.android.support:support-v4:$supportLibVersion"
    const val appcompat_v7 = "com.android.support:appcompat-v7:$supportLibVersion"
    const val design = "com.android.support:design:$supportLibVersion"
    const val recyclerview_v7 = "com.android.support:recyclerview-v7:$supportLibVersion"
    const val cardview_v7 = "com.android.support:cardview-v7:$supportLibVersion"
    const val play_auth = "com.google.android.gms:play-services-auth:18.0.0"
    const val play_firebase_analytics = "com.google.firebase:firebase-analytics:17.4.3  "
    const val play_firebase_core = "com.google.firebase:firebase-core:17.4.3"
    const val play_firebase_messaging = "com.google.firebase:firebase-messaging:20.2.0"
    const val play_firebase_config = "com.google.firebase:firebase-config:19.1.4"

    const val crashlytics = "com.google.firebase:firebase-crashlytics:17.0.1"
    const val timber = "com.jakewharton.timber:timber:4.7.1"
    const val rxjava2 = "io.reactivex.rxjava2:rxjava:2.2.19"
    const val junit = "junit:junit:4.13"
    const val espresso = "com.android.support.test.espresso:espresso-core:3.0.2"
    const val espressoIntents = "com.android.support.test.espresso:espresso-intents:3.0.2"
    const val test_runner = "com.android.support.test:runner:1.0.2"
    const val test_rules = "com.android.support.test:rules:1.0.2"
    const val ktx = "androidx.core:core-ktx:1.1.0"
    const val constraintLayout = "com.android.support.constraint:constraint-layout:1.1.3"
    const val paperDb = "io.paperdb:paperdb:2.6"
    const val rxAndroid2 = "io.reactivex.rxjava2:rxandroid:2.1.1"
    const val retrofit = "com.squareup.retrofit2:retrofit:2.6.2"
    const val retrofitCoroutines = "ru.gildor.coroutines:kotlin-coroutines-retrofit:1.1.0"
    const val retrofitRx2 = "com.squareup.retrofit2:adapter-rxjava2:2.6.2"
    const val pngj = "ar.com.hjg:pngj:2.1.0"
    const val okhttp = "com.squareup.okhttp3:okhttp:3.14.9"
    const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:3.14.9"
    const val multidex = "com.android.support:multidex:1.0.3"
    const val picassoDownloader = "com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0"
    const val picasso = "com.squareup.picasso:picasso:2.5.2"
    const val room = "android.arch.persistence.room:runtime:1.1.1"
    const val rxRoom = "android.arch.persistence.room:rxjava2:1.1.1"
    const val roomProcessor = "android.arch.persistence.room:compiler:1.1.1"
    const val flexbox = "com.google.android:flexbox:1.1.1"

    const val stdlibCommon = "org.jetbrains.kotlin:kotlin-stdlib-common" // stdlib metadata
    const val stdlibJvm = "org.jetbrains.kotlin:kotlin-stdlib"

    const val sqldelight = "com.squareup.sqldelight:android-driver:${Versions.sqldelight}"
    const val sqldelightCoroutinesExtensions = "com.squareup.sqldelight:coroutines-extensions:${Versions.sqldelight}"

    // See https://github.com/Kotlin/kotlinx.coroutines/issues/1096
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:${Versions.coroutines}"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val serializationRuntime = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:${Versions.serialization}"

    // See https://github.com/ktorio/ktor/issues/1619
    const val ktorClientCore = "io.ktor:ktor-client-core-native:${Versions.ktor}"
    const val ktorClientJson = "io.ktor:ktor-client-json-native:${Versions.ktor}"
    const val ktorClientSerialization = "io.ktor:ktor-client-serialization-native:${Versions.ktor}"
    const val ktorClientLogging = "io.ktor:ktor-client-logging-native:${Versions.ktor}"
    const val ktorClientEncoding = "io.ktor:ktor-client-encoding-native:${Versions.ktor}"
    const val ktorClientOkhttp = "io.ktor:ktor-client-okhttp:${Versions.ktor}"
    const val ktorClientCurl = "io.ktor:ktor-client-curl:${Versions.ktor}"

    const val okio = "com.squareup.okio:okio-multiplatform:2.5.0"

    const val klock = "com.soywiz.korlibs.klock:klock:1.9.1"

    const val kotlinTestCommon = "org.jetbrains.kotlin:kotlin-test-common"
    const val kotlinTestAnnotationCommon = "org.jetbrains.kotlin:kotlin-test-annotations-common"
    const val kotlinTestJvm = "org.jetbrains.kotlin:kotlin-test-junit"
    const val kotlinTestAnnotationJvm = "org.jetbrains.kotlin:kotlin-test-annotations"
}