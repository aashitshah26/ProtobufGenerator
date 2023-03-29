package com.protogen.core

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneOfMessage(
    val shouldGenerateSelf: Boolean,
    val childs: Array<KClass<out Any>> = [],
    val findOneOfChilds: Boolean = false
)
