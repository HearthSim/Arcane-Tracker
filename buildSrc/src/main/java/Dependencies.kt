object Versions {
    const val minSdkVersion = 16
    const val targetSdkVersion = 26
    const val compileSdkVersion = 26
    const val buildToolsVersion = "26.0.2"
    const val kotlin = "1.2.21"
    const val androidPlugin = "3.0.1"

}

object Libs {
    const val playServicesVersion = "11.6.0"
    const val supportLibVersion = "27.0.1"

    const val support_v4 = "com.android.support:support-v4:$supportLibVersion"
    const val appcompat_v7 = "com.android.support:appcompat-v7:$supportLibVersion"
    const val design = "com.android.support:design:$supportLibVersion"
    const val recyclerview_v7 = "com.android.support:recyclerview-v7:$supportLibVersion"
    const val cardview_v7 = "com.android.support:cardview-v7:$supportLibVersion"
    const val play_auth = "com.google.android.gms:play-services-auth:$playServicesVersion"
    const val play_firebase_core = "com.google.firebase:firebase-core:$playServicesVersion"
    const val play_firebase_crash = "com.google.firebase:firebase-crash:$playServicesVersion"
    const val play_firebase_messaging = "com.google.firebase:firebase-messaging:$playServicesVersion"
    const val gson = "com.google.code.gson:gson:2.8.2"
    const val moshi = "com.squareup.moshi:moshi:1.5.0"
    const val moshi_kotlin = "com.squareup.moshi:moshi-kotlin:1.5.0"
    const val timber = "com.jakewharton.timber:timber:4.5.1"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    const val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val rxjava2 = "io.reactivex.rxjava2:rxjava:2.1.6"
    const val rxkotlin = "io.reactivex:rxkotlin:1.0.0"
    const val junit = "junit:junit:4.12"
    const val espresso = "com.android.support.test.espresso:espresso-core:3.0.1"
    const val test_runner = "com.android.support.test:runner:1.0.1"
    const val test_rules = "com.android.support.test:rules:1.0.1"
    const val ktx = "androidx.core:core-ktx:0.1"
    const val constraintLayout = "com.android.support.constraint:constraint-layout:1.0.2"
    const val paperDb = "io.paperdb:paperdb:2.6"
    const val annimonStream = "com.annimon:stream:1.1.9"
    const val dataBindingCompiler = "com.android.databinding:compiler:${Versions.androidPlugin}"
    const val rxJava2 = "io.reactivex.rxjava2:rxjava:2.1.8"
    const val rxAndroid = "io.reactivex:rxandroid:1.2.1"
    const val rxKotlin = "io.reactivex:rxkotlin:1.0.0"
    const val retrofit = "com.squareup.retrofit2:retrofit:2.3.0"
    const val retrofitGson = "com.squareup.retrofit2:converter-gson:2.3.0"
    const val retrofitRx = "com.squareup.retrofit2:adapter-rxjava:2.3.0"
    const val pngj = "ar.com.hjg:pngj:2.1.0"
    const val kotlinPoet = "com.squareup:kotlinpoet:0.5.0"
    const val okhttp = "com.squareup.okhttp3:okhttp:3.9.1"


    const val multidex = "com.android.support:multidex:1.0.2"
    const val picassoDownloader = "com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0"
    const val picasso = "com.squareup.picasso:picasso:2.5.2"

}