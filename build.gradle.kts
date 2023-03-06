// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.andr.application) apply false
    alias(libs.plugins.andr.library) apply false
    alias(libs.plugins.ksp) apply false
    id("org.jetbrains.kotlin.jvm") version "1.7.10" apply false
}