package com.protogen.core

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneOfChild(val parent: KClass<*>)
