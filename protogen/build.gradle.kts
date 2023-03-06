plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.10-1.0.6")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation(project(":core"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    version = "1.0.0"
}