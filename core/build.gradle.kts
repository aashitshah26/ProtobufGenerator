plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}