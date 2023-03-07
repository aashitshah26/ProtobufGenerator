plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
    id("com.squareup.wire")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.protogen.genproto"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        getByName("debug") {
            resources.srcDirs("build/generated/ksp/debug/resources")
        }
        getByName("release") {
            resources.srcDirs("build/generated/ksp/release/resources")
        }
    }

    packagingOptions {
        resources.pickFirsts.add("**.proto")
    }
}

wire {
    sourcePath {
        srcDirs
        srcDir("build/generated/ksp/debug/resources/com/protogen")
    }
    kotlin {
        // to implement android.os.Parcelable
        android = true
    }
}

afterEvaluate {
    android.applicationVariants.forEach {
        val buildType = it.buildType.name
        val kspTask = "ksp${buildType.capitalize()}Kotlin"
        val wireTask = "generate${buildType.capitalize()}Protos"
        tasks.named(wireTask) {
            dependsOn(kspTask)
        }
    }
}

dependencies {
    implementation(libs.androidx.coreKtx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("io.github.aashitshah26:protobufgenerator-core:1.0.4")
    implementation(project(":core"))
    ksp(project(":protogen"))
//    implementation("io.github.aashitshah26:protobufgenerator-core:1.0.4")
//    ksp("io.github.aashitshah26:protobufgenerator-protogen:1.0.4")
}