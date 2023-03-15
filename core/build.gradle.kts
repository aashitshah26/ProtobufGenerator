plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}