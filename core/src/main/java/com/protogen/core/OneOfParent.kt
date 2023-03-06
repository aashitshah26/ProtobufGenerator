package com.protogen.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class OneOfParent(val shouldGenerateSelf: Boolean)
