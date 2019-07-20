plugins {
    java
    kotlin("jvm") version "1.3.41"
    kotlin("kapt") version "1.3.41"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.0.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
    implementation("com.squareup.moshi:moshi:1.8.0")
}