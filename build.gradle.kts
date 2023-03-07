// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.andr.application) apply false
    alias(libs.plugins.andr.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.mavenPublish) apply false
    id("org.jetbrains.kotlin.jvm") version "1.7.10" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.S01, true)
            signAllPublications()
        }
    }
}