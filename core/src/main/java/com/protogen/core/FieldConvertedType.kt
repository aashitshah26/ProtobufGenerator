package com.protogen.core

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class FieldConvertedType(
    val type: KClass<out Any>,
    val typeParams: Array<FieldConvertedType> = [],
    val isNullable: Boolean = false
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class FieldTypeParam(
    val nested: FieldTypeParam,
    val typeParam: Array<KClass<*>> = []
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class FieldConvertedType1(
    val type: KClass<*>
)