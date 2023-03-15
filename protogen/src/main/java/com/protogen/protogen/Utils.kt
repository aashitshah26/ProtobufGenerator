package com.protogen.protogen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import com.google.gson.annotations.SerializedName
import com.protogen.core.AutoProtoGenerator
import com.protogen.core.FieldConvertedType
import com.protogen.core.FieldConvertedType1
import com.protogen.core.IgnoreProtoProperty
import com.protogen.core.OneOfChild
import com.protogen.core.OneOfParent
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.createType


fun KSClassDeclaration.isSealed() =
    classKind == ClassKind.CLASS && modifiers.contains(Modifier.SEALED)

fun KSClassDeclaration.isEnum() = classKind == ClassKind.ENUM_CLASS

@OptIn(KspExperimental::class)
fun KSAnnotated.getSerializedName() =
    getAnnotationsByType(SerializedName::class)
        .firstOrNull()?.value

@OptIn(KspExperimental::class)
fun KSClassDeclaration.isOneOfParent() =
    getAnnotationsByType(OneOfParent::class).firstOrNull() != null

@OptIn(KspExperimental::class)
fun KSClassDeclaration.canGenerateSelf() =
    getAnnotationsByType(OneOfParent::class).firstOrNull()?.shouldGenerateSelf == true

fun Resolver.getOneOfChilds(declaration: KSClassDeclaration) =
    getSymbolsWithAnnotation(OneOfChild::class.qualifiedName.orEmpty())
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.CLASS }
        .filter { it.oneOfParentChecker(declaration) }

fun KSClassDeclaration.oneOfParentChecker(parent: KSClassDeclaration) =
    (annotations.firstOrNull {
        it.shortName.asString() == OneOfChild::class.simpleName
    }?.arguments?.first()?.value as? KSType)?.declaration?.let {
        it.simpleName.asString() == parent.simpleName.asString() &&
                it.qualifiedName?.asString() == parent.qualifiedName?.asString()
    } ?: false

fun KSClassDeclaration.isSubclassOf(cls: KClass<*>): Boolean =
    (simpleName.asString() == cls.simpleName && qualifiedName?.asString() == cls.qualifiedName) ||
            superTypes
                .map { it.resolve().declaration }
                .filter { (it as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                .firstOrNull {
                    cls.simpleName == it.simpleName.asString() && cls.qualifiedName == it.qualifiedName?.asString()
                }?.let { true } ?: false

fun KSClassDeclaration.isSubclassOf(simpleNameK: String, qualifiedNameK: String): Boolean =
    (simpleName.asString() == simpleNameK && qualifiedName?.asString() == qualifiedNameK) ||
            superTypes
                .map { it.resolve().declaration }
                .filter { (it as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                .firstOrNull {
                    simpleNameK == it.simpleName.asString() && qualifiedNameK == it.qualifiedName?.asString()
                }?.let { true } ?: false

fun Resolver.getSymbols(cls: KClass<*>) =
    getSymbolsWithAnnotation(cls.qualifiedName.orEmpty())
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.CLASS }

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getProtoOptions() =
    getAnnotationsByType(AutoProtoGenerator::class)
        .firstOrNull()?.let {
            ProtoOptions(javaPackage = it.javaPackage, javaMultipleFile = it.javaMultipleFile)
        } ?: ProtoOptions(javaPackage = packageName.asString() + ".protogen", javaMultipleFile = true)

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getAllProtoProperties() =
    getAllProperties()
        .filterNot { it.isAnnotationPresent(IgnoreProtoProperty::class) || it.isAnnotationPresent(Transient::class) }

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getEnumConstants() =
    declarations
        .filter { it is KSClassDeclaration }
        .filterNot { (it as KSClassDeclaration).isCompanionObject }
        .filterNot { it.isAnnotationPresent(IgnoreProtoProperty::class) || it.isAnnotationPresent(Transient::class) }

fun KSPropertyDeclaration.getConvertedType(resolver: Resolver, logger: KSPLogger) =
    annotations.firstOrNull {
        it.shortName.asString() == FieldConvertedType::class.simpleName
    }?.let { it.fetchType(resolver, logger) }
//        ?.arguments?.let {
//        var type = (it.getOrNull(0)?.value as? KSType)?.starProjection()
//        (it.getOrNull(1)?.value as? java.util.ArrayList<KSType>)?.let {
//            type = type?.replace(
//                it.map {
//                    resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(it.starProjection()), Variance.CONTRAVARIANT)
//                }
//            )
//        }
//        (it.getOrNull(2)?.value as? Boolean)?.let {
//            type = if (it) {
//                type?.makeNullable()
//            } else {
//                type?.makeNotNullable()
//            }
//        }
//        type!!
//    }


private fun KSAnnotation.fetchType(resolver: Resolver, logger: KSPLogger): KSType {
    return getFieldConvertedData(logger).let {
        if (it.annotations.isEmpty()) {
            if (it.isNullable) {
                it.type.makeNullable()
            } else {
                it.type.makeNotNullable()
            }
        } else {
            logger.warn("annotations: ${it.annotations}")
            val type = it.type.replace(
                it.annotations.map { annotation ->
                    resolver.getTypeArgument(
                        resolver.createKSTypeReferenceFromKSType(annotation.fetchType(resolver, logger)),
                        Variance.CONTRAVARIANT
                    )
                }
            )
            if (it.isNullable) {
                type.makeNullable()
            } else {
                type.makeNotNullable()
            }
            type
        }
    }
}

private fun KSAnnotation.getFieldConvertedData(logger: KSPLogger) = arguments.let {
    logger.warn("annotation_0 ${it.first { it.name?.asString() == "type" }.value}")
    logger.warn("annotation_1 ${it.first { it.name?.asString() == "typeParams" }.value}")
    logger.warn("annotation_2 ${it.first { it.name?.asString() == "isNullable" }.value}")

    FieldConvertedData(

        (it.first { it.name?.asString() == "type" }.value as KSType).starProjection(),
        (it.first { it.name?.asString() == "typeParams" }.value as java.util.ArrayList<KSAnnotation>),
        (it.first { it.name?.asString() == "isNullable" }.value as Boolean)
    )
}

private data class FieldConvertedData(
    val type: KSType,
    val annotations: List<KSAnnotation>,
    val isNullable: Boolean
)
