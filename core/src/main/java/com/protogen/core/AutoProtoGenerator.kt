package com.protogen.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoProtoGenerator(
    val javaPackage: String,
    val javaMultipleFile: Boolean = true
)
