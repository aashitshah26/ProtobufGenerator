package com.protogen.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneOfParent(val shouldGenerateSelf: Boolean)
